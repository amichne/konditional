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

val npmInstall = tasks.register<Exec>("npmInstall") {
    workingDir = projectDir
    commandLine("npm", "install")
    inputs.file("package.json")
    inputs.file("package-lock.json")
    outputs.dir("node_modules")
    isIgnoreExitValue = false
}

val ensureStaticDir = tasks.register("ensureStaticDir") {
    val staticDir = projectDir.resolve("src/main/resources/static")
    doLast {
        staticDir.mkdirs()
    }
}

val buildCss = tasks.register<Exec>("buildCss") {
    dependsOn(npmInstall, ensureStaticDir)
    workingDir = projectDir
    commandLine("npm", "run", "build:css")
    inputs.file("src/main/resources/css/input.css")
    inputs.file("tailwind.config.js")
    inputs.dir("src/main/kotlin")
    outputs.file("src/main/resources/static/styles.css")
    isIgnoreExitValue = false
}

tasks.processResources {
    dependsOn(buildCss)
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
