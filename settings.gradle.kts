pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "konditional"

include("core")
include("kontracts")
include("konditional-opentelemetry")
include("ktor-demo")
include("ktor-demo:demo-client")
