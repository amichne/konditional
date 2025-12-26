// Kotlin/JS React component library for Konditional configuration UIs.
// This module provides reusable, type-safe React components for editing feature flag configurations.
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
        binaries.library()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
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

                // Drag-and-drop for array reordering
                implementation(npm("@dnd-kit/core", "6.1.0"))
                implementation(npm("@dnd-kit/sortable", "8.0.0"))
                implementation(npm("@dnd-kit/utilities", "3.2.2"))
            }
        }
    }
}
