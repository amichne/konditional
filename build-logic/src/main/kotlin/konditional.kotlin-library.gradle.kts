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
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=1.8")
        optIn.add("io.amichne.konditional.api.KonditionalInternalApi")
    }
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}
