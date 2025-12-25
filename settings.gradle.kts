pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "konditional"

include("kontracts")
include("ktor-demo")
include("ktor-demo:demo-client")
include("ktor-demo:ui-slices")
