import io.amichne.konditional.gradle.GenerateRecipesDocsTask
import io.amichne.konditional.gradle.KonditionalRecipesDocsExtension
import io.amichne.konditional.gradle.VerifyRecipesDocsTask

val extension = extensions.create<KonditionalRecipesDocsExtension>("konditionalRecipesDocs")

val generateRecipesDocs = tasks.register<GenerateRecipesDocsTask>("generateRecipesDocs") {
    group = "documentation"
    description = "Generate recipes docs from Kotlin samples."
    sampleFile.set(extension.sampleFile)
    templateFile.set(extension.templateFile)
    outputFile.set(extension.docsFile)
}

val verifyRecipesDocs = tasks.register<VerifyRecipesDocsTask>("verifyRecipesDocs") {
    group = "verification"
    description = "Verify recipes docs are up-to-date with Kotlin samples."
    sampleFile.set(extension.sampleFile)
    templateFile.set(extension.templateFile)
    docsFile.set(extension.docsFile)
}

afterEvaluate {
    require(extension.sampleFile.isPresent) {
        "konditionalRecipesDocs.sampleFile must be set"
    }
    require(extension.templateFile.isPresent) {
        "konditionalRecipesDocs.templateFile must be set"
    }
    require(extension.docsFile.isPresent) {
        "konditionalRecipesDocs.docsFile must be set"
    }
}

tasks.named("test") {
    dependsOn("compileDocsSamplesKotlin")
}

tasks.named("check") {
    dependsOn(verifyRecipesDocs)
}
