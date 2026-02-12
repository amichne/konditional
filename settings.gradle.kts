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
include("konditional-spec")
include("kontracts")
include("opentelemetry")
include("detekt-rules")
include("openfeature")
