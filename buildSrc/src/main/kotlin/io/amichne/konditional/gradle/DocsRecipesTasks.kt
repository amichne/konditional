package io.amichne.konditional.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateRecipesDocsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sampleFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val snippets = extractRecipeSnippets(sampleFile.asFile.get().readText())
        val rendered = renderRecipesDoc(templateFile.asFile.get().readText(), snippets)
        val output = outputFile.asFile.get()
        output.parentFile.mkdirs()
        output.writeText(rendered)
    }
}

abstract class VerifyRecipesDocsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sampleFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val snippets = extractRecipeSnippets(sampleFile.asFile.get().readText())
        val rendered = renderRecipesDoc(templateFile.asFile.get().readText(), snippets)
        val current = docsFile.asFile.get().readText()
        check(rendered == current) {
            "Recipes docs are out of date. Run :konditional-observability:generateRecipesDocs."
        }
    }
}

private fun extractRecipeSnippets(source: String): Map<String, String> {
    val snippets = mutableMapOf<String, String>()
    var activeName: String? = null
    val buffer = mutableListOf<String>()

    source.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("// region ")) {
            check(activeName == null) { "Nested region start detected: $trimmed" }
            activeName = trimmed.removePrefix("// region ").trim()
            buffer.clear()
            return@forEach
        }
        if (trimmed.startsWith("// endregion ")) {
            val name = trimmed.removePrefix("// endregion ").trim()
            check(activeName == name) { "Region end mismatch: expected $activeName, got $name" }
            val snippet = buffer.joinToString("\n").trimIndent().trim()
            check(snippet.isNotEmpty()) { "Empty snippet for region $name" }
            check(snippets.put(name, snippet) == null) { "Duplicate snippet for region $name" }
            activeName = null
            buffer.clear()
            return@forEach
        }
        if (activeName != null) {
            buffer.add(line)
        }
    }

    check(activeName == null) { "Unclosed region in recipes samples: $activeName" }
    return snippets.toMap()
}

private fun renderRecipesDoc(template: String, snippets: Map<String, String>): String {
    var rendered = template
    snippets.forEach { (name, snippet) ->
        val token = "{{${name}}}"
        check(rendered.contains(token)) { "Missing placeholder token $token in recipes template" }
        val codeFence = "```kotlin\n$snippet\n```"
        rendered = rendered.replace(token, codeFence)
    }
    check(!rendered.contains("{{")) { "Unresolved placeholder tokens in recipes output" }
    return rendered.trimEnd() + "\n"
}
