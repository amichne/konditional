pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "konditional"

include("core")
// New modules (work in progress - commented out due to circular dependencies)
include("konditional-core")
include("konditional-serialization")
include("konditional-runtime")
include("konditional-observability")
include("config-metadata")
include("kontracts")
include("openapi")
include("opentelemetry")
include("detekt-rules")
