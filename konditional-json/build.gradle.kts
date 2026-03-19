plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":konditional-engine"))

    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(testFixtures(project(":konditional-engine")))
}
