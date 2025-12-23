// Note: Using kotlin("js") plugin (deprecated but functional)
// Upgraded to Kotlin 2.2.20 which fixes JS IR compiler bugs (KT-77372)
// Future: migrate to kotlin("multiplatform") with js() target
plugins {
    kotlin("js")
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
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.10.2")
}
