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
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

                // Kotlin/JS React + MUI wrappers via the JetBrains kotlin-wrappers version catalog.
                implementation(kotlinWrappers.react)
                implementation(kotlinWrappers.reactDom)
                implementation(kotlinWrappers.emotion.react)
                implementation(kotlinWrappers.emotion.styled)
                implementation(kotlinWrappers.web)
                implementation(kotlinWrappers.mui.material)
                implementation(kotlinWrappers.mui.system)
                implementation(kotlinWrappers.mui.iconsMaterial)
            }
        }
    }
}
