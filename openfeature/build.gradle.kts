plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    api(project(":konditional-core"))
    implementation(project(":konditional-runtime"))
    api(libs.bundles.openfeature)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

konditionalPublishing {
    artifactId.set("openfeature")
    moduleName.set("Konditional OpenFeature")
    moduleDescription.set("OpenFeature provider implementation for Konditional feature flags")
}
