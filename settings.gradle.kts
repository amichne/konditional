pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
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
