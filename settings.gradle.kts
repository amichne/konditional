pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.vanniktech.maven.publish") version "0.35.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "konditional"

include("konditional-core")
include("konditional-serialization")
include("konditional-runtime")
include("konditional-observability")
include("server:rest-spec")
include("server:ktor")
include("kontracts")
include("openapi")
include("opentelemetry")
include("detekt-rules")
include("openfeature")
