plugins {
    alias(libs.plugins.android.library)
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

android {
    namespace = "com.axiom.models"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
                artifactId = "axiom-models"
                version = "1.0.0"

                pom {
                    name.set("Axiom Models")
                    description.set("Model definitions and configurations for Axiom LLM SDK")
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
