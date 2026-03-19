pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "konditional"

include("konditional-types")
include("konditional-engine")
include("konditional-json")
include("kontracts")
include("smoke-test")

// Legacy source trees remain in-repo as a reference during extraction but are no longer included.
