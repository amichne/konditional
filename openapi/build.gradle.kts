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
    artifactId.set("openapi")
    moduleName.set("OpenApi Schema Types")
    moduleDescription.set("OpenAPI schema interfaces shared by Kontracts")
}
