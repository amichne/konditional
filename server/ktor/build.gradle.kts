plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
    application
}

dependencies {
    implementation(project(":server:core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.test.host)
}

application {
    mainClass.set("io.amichne.konditional.server.ktor.spec.KonditionalSpecServerKt")
}

konditionalPublishing {
    artifactId.set("konditional-ktor-spec")
    moduleName.set("Konditional Server Ktor")
    moduleDescription.set("Ktor route module and standalone server implementation for the Konditional REST surface")
}
