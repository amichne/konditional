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
include("kontracts")
include("openapi")
include("konditional-otel")
include("detekt-rules")
include("openfeature")
include("konditional-http-server")
