 pluginManagement {
    val hoverPluginVersion: String by settings

    repositories {
        mavenLocal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.komunasuarus.hovermaps") {
                useVersion(hoverPluginVersion)
            }
        }
    }

    plugins {
        id("com.vanniktech.maven.publish") version "0.35.0"
    }
}

rootProject.name = "konditional"

include("konditional-core")
include("konditional-serialization")
include("konditional-runtime")
include("konditional-observability")
include("config-metadata")
include("kontracts")
include("openapi")
include("opentelemetry")
include("detekt-rules")
