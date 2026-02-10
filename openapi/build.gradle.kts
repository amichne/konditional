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
    artifactId.set("konditional-openapi")
    moduleName.set("Konditional OpenAPI Schema Types")
    moduleDescription.set("OpenAPI schema interfaces shared by Kontracts")
}
