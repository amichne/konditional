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

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    api(project(":konditional-core"))
    implementation(project(":konditional-runtime"))
    api("dev.openfeature:sdk:1.9.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

configureKonditionalPublishing(
    artifactId = "openfeature",
    moduleName = "Konditional OpenFeature",
    moduleDescription = "OpenFeature provider implementation for Konditional feature flags",
)
