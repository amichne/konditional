plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":konditional-types"))
    api(project(":kontracts"))
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testFixturesImplementation(project(":konditional-types"))
}
