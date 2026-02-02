plugins {
    id("konditional.kotlin-library")
    id("konditional.publishing")
    id("konditional.detekt")
    id("konditional.recipes-docs")
    id("konditional.junit-platform")
}

dependencies {
    // Core and runtime modules
    api(project(":konditional-core"))
    api(project(":konditional-runtime"))

    // Kotlin stdlib
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.test)
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

konditionalRecipesDocs {
    sampleFile.set(recipesSampleFile)
    templateFile.set(recipesTemplateFile)
    docsFile.set(recipesDocFile)
}

konditionalPublishing {
    artifactId.set("konditional-observability")
    moduleName.set("Konditional Observability")
    moduleDescription.set("Observability, monitoring, and recipe documentation for Konditional feature flags")
}
