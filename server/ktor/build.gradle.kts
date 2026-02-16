plugins {
    application
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.junit-platform")
}

dependencies {
    api(project(":server:rest-spec"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.test.host)
}

konditionalPublishing {
    artifactId.set("server-ktor")
    moduleName.set("Konditional Server Ktor")
    moduleDescription.set("Ktor integration module exposing Konditional REST specification endpoints")
}

application {
    mainClass.set("io.amichne.konditional.server.ktor.spec.KonditionalRestSpecStandaloneServer")
}
