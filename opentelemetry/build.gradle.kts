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
    // Core konditional dependency
    api(project(":konditional-core"))
    implementation(project(":konditional-runtime"))

    // OpenTelemetry API (lightweight, propagates to consumers)
    api("io.opentelemetry:opentelemetry-api:1.34.1")

    // OpenTelemetry SDK (implementation detail)
    implementation("io.opentelemetry:opentelemetry-sdk:1.34.1")
    implementation("io.opentelemetry:opentelemetry-semconv:1.23.1-alpha")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.34.1")
}

tasks.test {
    useJUnitPlatform()
}

configureKonditionalPublishing(
    artifactId = "opentelemetry",
    moduleName = "Konditional OpenTelemetry",
    moduleDescription = "OpenTelemetry instrumentation for Konditional feature flag evaluation",
)
