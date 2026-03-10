plugins {
    id("konditional.kotlin-library")
    id("konditional.junit-platform")
}

dependencies {
    api(project(":konditional"))

    implementation(kotlin("test"))
    implementation(libs.junit.jupiter)
}
