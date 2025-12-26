// Kotlin/JS client built as a single JS executable (bundled via webpack) for the Ktor demo server.
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}


kotlin {
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // Minimal dependencies for vanilla Kotlin/JS
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

                // konditional-ui for DTOs and models (UI logic rewritten in vanilla)
                implementation(project(":konditional-ui"))
            }
        }
    }
}
