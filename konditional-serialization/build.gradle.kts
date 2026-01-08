import io.amichne.konditional.gradle.configureKonditionalPublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
    id("io.gitlab.arturbosch.detekt")
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
    // Core module
    api(project(":konditional-core"))

    // Kontracts for JSON schema types
    api(project(":kontracts"))

    // Config metadata
    implementation(project(":config-metadata"))

    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Kotlin stdlib
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation(project(":konditional-runtime"))
    testImplementation(testFixtures(project(":konditional-runtime")))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Test")) {
        compilerOptions.optIn.add("io.amichne.konditional.internal.KonditionalInternalApi")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configureKonditionalPublishing(
    artifactId = "konditional-serialization",
    moduleName = "Konditional Serialization",
    moduleDescription = "JSON serialization and deserialization support for Konditional feature flags"
)
