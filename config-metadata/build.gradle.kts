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
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

configureKonditionalPublishing(
    artifactId = "config-metadata",
    moduleName = "Config Metadata",
    moduleDescription = "Configuration metadata model for Konditional serialization and validation",
)
