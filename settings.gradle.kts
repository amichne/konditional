pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "konditional"

include("core")
include("config-metadata")
include("kontracts")
include("openapi")
include("opentelemetry")
include("detekt-rules")
