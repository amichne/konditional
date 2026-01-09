import io.amichne.konditional.gradle.KonditionalCoreApiBoundaryTask
import io.amichne.konditional.gradle.configureKonditionalPublishing

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
    id("io.gitlab.arturbosch.detekt")
    `java-test-fixtures`
}

val props = project.rootProject.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

// Friend paths removed - using @KonditionalInternalApi instead

repositories {
    mavenCentral()
}

dependencies {
    // Kontracts: Type-safe JSON Schema DSL
    implementation(project(":kontracts"))
    implementation(project(":config-metadata"))

    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Kotlin stdlib and coroutines
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation(project(":konditional-serialization"))
    testImplementation(project(":konditional-runtime"))
    testImplementation(testFixtures(project(":konditional-core")))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configureKonditionalPublishing(
    artifactId = "konditional-core",
    moduleName = "Konditional Core",
    moduleDescription = "Core feature flag evaluation engine with type-safe API and deterministic evaluation"
)

// ============================================================================
// API Boundary Policy (package allowlist)
// ============================================================================

val konditionalCoreAllowedPackagePrefixes =
    listOf(
        "io.amichne.konditional.api",
        "io.amichne.konditional.context",
        "io.amichne.konditional.core",
        "io.amichne.konditional.rules",
        "io.amichne.konditional.values",
        "io.amichne.konditional.internal",
    )

tasks.register<KonditionalCoreApiBoundaryTask>("checkKonditionalCoreApiBoundary") {
    group = "verification"
    description = "Ensures :konditional-core source packages stay within the declared allowlist."
    allowedPackagePrefixes.set(konditionalCoreAllowedPackagePrefixes)
    sourceDir.set(layout.projectDirectory.dir("src/main/kotlin"))
    projectDir.set(layout.projectDirectory)
}

tasks.named("check") {
    dependsOn("checkKonditionalCoreApiBoundary")
}

tasks.named("compileKotlin") {
    dependsOn("checkKonditionalCoreApiBoundary")
}
