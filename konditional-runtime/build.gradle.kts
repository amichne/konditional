import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.detekt")
    id("konditional.junit-platform")
    `java-test-fixtures`
}

dependencies {
    // Core and serialization modules
    api(project(":konditional-core"))
    api(project(":konditional-serialization"))

    // Kotlin stdlib and coroutines
    implementation(kotlin("reflect"))
    implementation(libs.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.moshi)
    testImplementation(testFixtures(project(":konditional-core")))
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Test")) {
        compilerOptions.optIn.add("io.amichne.konditional.internal.KonditionalInternalApi")
    }
}

konditionalPublishing {
    artifactId.set("konditional-runtime")
    moduleName.set("Konditional Runtime")
    moduleDescription.set("Runtime execution engine and evaluation pipeline for Konditional feature flags")
}
