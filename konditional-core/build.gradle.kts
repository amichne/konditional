plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.detekt")
    id("konditional.core-api-boundary")
    id("konditional.junit-platform")
    `java-test-fixtures`
}

dependencies {
    // Kontracts: Type-safe JSON Schema DSL
    implementation(project(":kontracts"))
    implementation(project(":config-metadata"))

    // Moshi for JSON serialization
    implementation(libs.bundles.moshi)

    // Kotlin stdlib and coroutines
    implementation(kotlin("reflect"))
    implementation(libs.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
    testImplementation(project(":konditional-serialization"))
    testImplementation(project(":konditional-runtime"))
    testImplementation(testFixtures(project(":konditional-core")))
}

// ============================================================================
// API Boundary Policy (package allowlist)
// ============================================================================

konditionalPublishing {
    artifactId.set("konditional-core")
    moduleName.set("Konditional Core")
    moduleDescription.set("Core feature flag evaluation engine with type-safe API and deterministic evaluation")
}

val konditionalCoreAllowedPackagePrefixes =
    listOf(
        "io.amichne.konditional.api",
        "io.amichne.konditional.context",
        "io.amichne.konditional.core",
        "io.amichne.konditional.rules",
        "io.amichne.konditional.values",
    )

konditionalCoreApiBoundary {
    allowedPackagePrefixes.set(konditionalCoreAllowedPackagePrefixes)
}
