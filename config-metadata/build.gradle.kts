plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

konditionalPublishing {
    artifactId.set("konditional-config-metadata")
    moduleName.set("Konditional Config Metadata")
    moduleDescription.set("Configuration metadata model for Konditional serialization and validation")
}
