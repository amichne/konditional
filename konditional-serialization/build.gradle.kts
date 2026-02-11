import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.detekt")
    id("konditional.junit-platform")
}

dependencies {
    // Core module
    api(project(":konditional-core"))

    // Kontracts for JSON schema types
    api(project(":kontracts"))

    // Moshi for JSON serialization
    implementation(libs.bundles.moshi)

    // Kotlin stdlib
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(project(":konditional-runtime"))
    testImplementation(testFixtures(project(":konditional-core")))
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Test")) {
        compilerOptions.optIn.add("io.amichne.konditional.internal.KonditionalInternalApi")
    }
}

konditionalPublishing {
    artifactId.set("konditional-serialization")
    moduleName.set("Konditional Serialization")
    moduleDescription.set("JSON serialization and deserialization support for Konditional feature flags")
}
