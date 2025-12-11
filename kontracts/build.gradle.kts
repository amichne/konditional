plugins {
    kotlin("jvm")
    `java-library`
}

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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Zero dependencies except Kotlin stdlib
    testImplementation(kotlin("test"))
}
