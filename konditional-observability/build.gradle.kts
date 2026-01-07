import io.amichne.konditional.gradle.GenerateRecipesDocsTask
import io.amichne.konditional.gradle.VerifyRecipesDocsTask

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

// Friend paths removed - using @KonditionalInternalApi instead

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

sourceSets {
    val main by getting
    create("docsSamples") {
        java.srcDir("src/docsSamples/kotlin")
        compileClasspath += main.output + main.compileClasspath
        runtimeClasspath += output + compileClasspath
    }
}

val recipesSampleFile =
    layout.projectDirectory.file(
        "src/docsSamples/kotlin/io/amichne/konditional/docsamples/RecipesSamples.kt"
    )
val recipesTemplateFile =
    rootProject.layout.projectDirectory.file("docusaurus/docs-templates/recipes.template.md")
val recipesDocFile =
    rootProject.layout.projectDirectory.file("docusaurus/docs/advanced/recipes.md")

val generateRecipesDocs by tasks.registering(GenerateRecipesDocsTask::class) {
    group = "documentation"
    description = "Generate recipes docs from Kotlin samples."
    sampleFile.set(recipesSampleFile)
    templateFile.set(recipesTemplateFile)
    outputFile.set(recipesDocFile)
}

val verifyRecipesDocs by tasks.registering(VerifyRecipesDocsTask::class) {
    group = "verification"
    description = "Verify recipes docs are up-to-date with Kotlin samples."
    sampleFile.set(recipesSampleFile)
    templateFile.set(recipesTemplateFile)
    docsFile.set(recipesDocFile)
}

tasks.named("test") {
    dependsOn("compileDocsSamplesKotlin", verifyRecipesDocs)
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
