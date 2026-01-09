import io.amichne.konditional.gradle.configureKonditionalPublishing

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
}

val props = project.rootProject.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":openapi"))

    // Zero dependencies except Kotlin stdlib
    testImplementation(kotlin("test"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

configureKonditionalPublishing(
    artifactId = "kontracts",
    moduleName = "Kontracts",
    moduleDescription = "Type-safe JSON Schema DSL for Konditional structured value validation",
)
