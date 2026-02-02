plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    api(project(":openapi"))

    implementation(kotlin("reflect"))

    // Zero dependencies except Kotlin stdlib
    testImplementation(kotlin("test"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

konditionalPublishing {
    artifactId.set("kontracts")
    moduleName.set("Kontracts")
    moduleDescription.set("Type-safe JSON Schema DSL for Konditional structured value validation")
}
