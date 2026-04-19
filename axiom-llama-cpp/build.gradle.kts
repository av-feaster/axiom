plugins {
    alias(libs.plugins.android.library)
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

android {
    namespace = "com.axiom.llama.cpp"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // NDK r27+ required for 16 KB page-size ELF alignment (Play / device checks).
        ndkVersion = "27.0.12077973"
    }
    
    // External build configuration for CMake
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }
    
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
                arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                abiFilters += "arm64-v8a"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    ndkVersion = "26.1.10909125"

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.av-feaster"
                artifactId = "axiom-llama-cpp"
                version = "1.0.0"

                pom {
                    name.set("Axiom Llama.cpp")
                    description.set("Native Llama.cpp library integration for Axiom LLM SDK")
                    url.set("https://github.com/av-feaster/axiom")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("av-feaster")
                            name.set("Axiom Team")
                            email.set("contact@axiom.ai")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/av-feaster/axiom.git")
                        developerConnection.set("scm:git:ssh://github.com/av-feaster/axiom.git")
                        url.set("https://github.com/av-feaster/axiom")
                    }
                }
            }
        }
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
    }
}

afterEvaluate {
    signing {
        sign(publishing.publications["release"])
    }
}

dependencies {
    implementation(project(":axiom-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
