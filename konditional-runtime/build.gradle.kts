import io.amichne.konditional.gradle.configureKonditionalPublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    `java-test-fixtures`
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
    // Core and serialization modules
    api(project(":konditional-core"))
    api(project(":konditional-serialization"))

    // Kotlin stdlib and coroutines
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("com.squareup.moshi:moshi:1.15.0")
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
    artifactId = "konditional-runtime",
    moduleName = "Konditional Runtime",
    moduleDescription = "Runtime execution engine and evaluation pipeline for Konditional feature flags"
)
