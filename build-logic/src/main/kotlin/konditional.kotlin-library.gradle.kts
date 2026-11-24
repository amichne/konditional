import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-library`
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("version")
    .orElse(providers.gradleProperty("VERSION"))
    .get()

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(27)
    compilerOptions {
        // Kotlin 2.4.21 supports JVM bytecode through Java 26.
        jvmTarget.set(JvmTarget.JVM_26)
        freeCompilerArgs.add("-Xjdk-release=26")
        optIn.add("io.amichne.konditional.api.KonditionalInternalApi")
    }
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_26
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(26)
}
