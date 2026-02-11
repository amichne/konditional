file=build-logic/src/main/kotlin/io/amichne/konditional/gradle/DocsRecipesTasks.kt
package=io.amichne.konditional.gradle
imports=org.gradle.api.DefaultTask,org.gradle.api.file.RegularFileProperty,org.gradle.api.tasks.InputFile,org.gradle.api.tasks.OutputFile,org.gradle.api.tasks.PathSensitive,org.gradle.api.tasks.PathSensitivity,org.gradle.api.tasks.TaskAction
type=io.amichne.konditional.gradle.GenerateRecipesDocsTask|kind=class|decl=abstract class GenerateRecipesDocsTask : DefaultTask()
type=io.amichne.konditional.gradle.VerifyRecipesDocsTask|kind=class|decl=abstract class VerifyRecipesDocsTask : DefaultTask()
fields:
- abstract val sampleFile: RegularFileProperty
- abstract val templateFile: RegularFileProperty
- abstract val outputFile: RegularFileProperty
- abstract val sampleFile: RegularFileProperty
- abstract val templateFile: RegularFileProperty
- abstract val docsFile: RegularFileProperty
methods:
- fun generate()
- fun verify()
