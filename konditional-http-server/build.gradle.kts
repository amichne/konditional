plugins {
    id("konditional.kotlin-library")
    id("konditional.junit-platform")
    application
}

dependencies {
    implementation(project(":konditional-core"))
    implementation(project(":konditional-runtime"))
    implementation(project(":konditional-serialization"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.moshi)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
}

application {
    mainClass.set("io.amichne.konditional.httpserver.MainKt")
}
