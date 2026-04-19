pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/av-feaster/axiom")
            credentials {
                username = System.getenv("GITHUB_USERNAME") ?: providers.gradleProperty("gpr.user").orElse("av-feaster").get()
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orElse("").get()
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Axiom"
include(":app")
include(":sample")
include(":axiom-core")
include(":axiom-llama-cpp")
include(":axiom-models")
include(":axiom-android-sdk")