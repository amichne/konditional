plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Core konditional dependency
    api(project(":core"))

    // OpenTelemetry API (lightweight, propagates to consumers)
    api("io.opentelemetry:opentelemetry-api:1.34.1")

    // OpenTelemetry SDK (implementation detail)
    implementation("io.opentelemetry:opentelemetry-sdk:1.34.1")
    implementation("io.opentelemetry:opentelemetry-semconv:1.23.1-alpha")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.34.1")
    testImplementation(testFixtures(project(":core")))
}

tasks.test {
    useJUnitPlatform()
}
