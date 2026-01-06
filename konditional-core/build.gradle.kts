
plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
    id("io.gitlab.arturbosch.detekt")
}

val props = project.rootProject.properties
group = props["GROUP"] as String
version = props["VERSION"] as String

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    // WIP split: tests are not migrated to the split modules yet (they still live in `:core`).
    sourceSets {
        val test by getting {
            kotlin.setSrcDirs(listOf("src/disabled-test/kotlin"))
            resources.setSrcDirs(listOf("src/disabled-test/resources"))
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kontracts: Type-safe JSON Schema DSL
    implementation(project(":kontracts"))
    implementation(project(":config-metadata"))

    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")

    // Kotlin stdlib and coroutines
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = props["GROUP"] as String
            artifactId = "konditional-core"
            version = props["VERSION"] as String
        }
    }
}
