pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
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
include("ui-spec")
include("ui-ktor")
include("demo")
