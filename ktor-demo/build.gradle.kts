plugins {
    kotlin("jvm") version "2.2.0"
    id("io.ktor.plugin") version "3.0.1"
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Konditional library (from main project)
    implementation(project(":"))

    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:3.0.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.1")
    implementation("io.ktor:ktor-server-html-builder-jvm:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.1")

    // kotlinx.html
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    // Moshi (for JSON handling, matching main project)
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}

application {
    mainClass.set("io.amichne.konditional.demo.ApplicationKt")
}
