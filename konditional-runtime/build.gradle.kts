
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
}

repositories {
    mavenCentral()
}

dependencies {
    // Core and serialization modules
    api(project(":konditional-core"))
    api(project(":konditional-serialization"))

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
            artifactId = "konditional-runtime"
            version = props["VERSION"] as String
        }
    }
}
