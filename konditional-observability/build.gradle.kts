
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

// Allow this module to use `internal` implementation details from `konditional-core`
// without widening the public API surface.
val konditionalCoreClassesDir = project(":konditional-core").layout.buildDirectory.dir("classes/kotlin/main")
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add(
        konditionalCoreClassesDir.map { "-Xfriend-paths=${it.asFile.absolutePath}" },
    )
}

repositories {
    mavenCentral()
}

dependencies {
    // Core and runtime modules
    api(project(":konditional-core"))
    api(project(":konditional-runtime"))

    // Kotlin stdlib
    implementation(kotlin("reflect"))

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
            artifactId = "konditional-observability"
            version = props["VERSION"] as String
        }
    }
}
