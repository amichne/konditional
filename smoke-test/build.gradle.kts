plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(project(":konditional-json"))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}
