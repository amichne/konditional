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
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val ktorVersion = "2.3.12"

dependencies {
    api(project(":konditional-serialization"))
    api(project(":ui-spec"))
    api(project(":konditional-runtime"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

val reactUiDist = rootProject.layout.projectDirectory.dir("konditional-generated-ui/dist")

tasks.processResources {
    from(reactUiDist) {
        into("ui")
    }
}

tasks.test {
    useJUnitPlatform()
}

configureKonditionalPublishing(
    artifactId = "ui-ktor",
    moduleName = "UI Ktor",
    moduleDescription = "Ktor + htmx rendering scaffold for Konditional UI spec",
)
