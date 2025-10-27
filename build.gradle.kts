plugins {
    kotlin("jvm") version "2.2.0"
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}
