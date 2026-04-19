# Maven Publishing Checklist for Axiom Modules

This document provides a step-by-step guide for publishing all Axiom modules to GitHub Packages.

## Prerequisites

1. **GitHub Personal Access Token (PAT)**
   - Go to https://github.com/settings/tokens
   - Click "Generate new token" → "Generate new token (classic)"
   - Generate a new token with these scopes:
     - `write:packages` - Required for publishing
     - `read:packages` - Required for reading packages
   - Save this token securely (you won't see it again)

2. **Set Environment Variables** (Recommended)
   ```bash
   export GITHUB_USERNAME=av-feaster
   export GITHUB_TOKEN=your-pat-token-here
   ```

   Alternatively, add to `gradle.properties` (not recommended for version control):
   ```properties
   gpr.user=av-feaster
   gpr.key=your-pat-token-here
   ```

## Publishing Order

Publish modules in this order to handle dependencies correctly:

### 1. Publish axiom-core (Pure Kotlin)

```bash
./gradlew :axiom-core:publishReleasePublicationToGitHubPackagesRepository
```

**Verification:**
- Check GitHub Packages → `axiom-core` artifact
- Version: `1.0.0`
- Group ID: `com.github.av-feaster`

### 2. Publish axiom-models (Pure Kotlin, depends on axiom-core)

```bash
./gradlew :axiom-models:publishReleasePublicationToGitHubPackagesRepository
```

**Verification:**
- Check GitHub Packages → `axiom-models` artifact
- Version: `1.0.0`
- Group ID: `com.github.av-feaster`

### 3. Publish axiom-llama-cpp (Native Library, depends on axiom-core)

```bash
./gradlew :axiom-llama-cpp:publishReleasePublicationToGitHubPackagesRepository
```

**Verification:**
- Check GitHub Packages → `axiom-llama-cpp` artifact
- Version: `1.0.0`
- Group ID: `com.github.av-feaster`
- Includes native `.so` libraries in AAR

### 4. Update axiom-android-sdk dependencies

Edit `axiom-android-sdk/build.gradle.kts`:

Replace project dependencies with Maven artifacts:

```kotlin
dependencies {
    // OLD (project dependencies):
    // implementation(project(":axiom-core"))
    // implementation(project(":axiom-models"))
    // implementation(project(":axiom-llama-cpp"))

    // NEW (Maven artifacts):
    implementation("com.github.av-feaster:axiom-core:1.0.0")
    implementation("com.github.av-feaster:axiom-models:1.0.0")
    implementation("com.github.av-feaster:axiom-llama-cpp:1.0.0")

    // ... rest of dependencies
}
```

Also add GitHub Packages repository to the project:

In `settings.gradle.kts` or root `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/av-feaster/axiom")
        credentials {
            username = System.getenv("GITHUB_USERNAME") ?: project.findProperty("gpr.user") as String? ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
        }
    }
    google()
    mavenCentral()
}
```

### 5. Publish axiom-android-sdk

```bash
./gradlew :axiom-android-sdk:publishReleasePublicationToGitHubPackagesRepository
```

**Verification:**
- Check GitHub Packages → `axiom-android-sdk` artifact
- Version: `1.0.0`
- Group ID: `com.github.av-feaster`

## Using Published Artifacts in Other Projects

To use the published artifacts in another Android project, add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/av-feaster/axiom")
        credentials {
            username = System.getenv("GITHUB_USERNAME") ?: project.findProperty("gpr.user") as String? ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
        }
    }
}

dependencies {
    implementation("com.github.av-feaster:axiom-android-sdk:1.0.0")
}
```

## Troubleshooting

### Authentication Errors
- Ensure your PAT has `write:packages` and `read:packages` scopes
- Verify environment variables are set correctly
- Check that credentials are properly configured in gradle.properties

### Build Failures
- Run `./gradlew clean` before publishing
- Ensure all modules build successfully: `./gradlew build`
- Check for compilation errors in each module

### Native Library Issues (axiom-llama-cpp)
- Ensure NDK is properly configured
- Verify CMake builds successfully: `./gradlew :axiom-llama-cpp:assembleRelease`
- Check that native libraries are included in the AAR

## Version Management

To publish a new version:
1. Update the `version` field in each module's `build.gradle.kts`
2. Follow the publishing order above
3. Consider using semantic versioning (e.g., `1.0.1`, `1.1.0`, `2.0.0`)

## Summary of Published Artifacts

| Module | Artifact ID | Version | Dependencies |
|--------|-------------|---------|--------------|
| axiom-core | axiom-core | 1.0.0 | None |
| axiom-models | axiom-models | 1.0.0 | axiom-core |
| axiom-llama-cpp | axiom-llama-cpp | 1.0.0 | axiom-core |
| axiom-android-sdk | axiom-android-sdk | 1.0.0 | axiom-core, axiom-models, axiom-llama-cpp |
