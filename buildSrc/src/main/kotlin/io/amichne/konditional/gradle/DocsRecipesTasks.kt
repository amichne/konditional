package io.amichne.konditional.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateRecipesDocsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sampleFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val snippets = extractRecipeSnippets(sampleFile.asFile.get().readText())
        val template = templateFile.asFile.get().readText()
        val sections = splitTemplateIntoSections(template)

        val outDir = outputDir.asFile.get()
        outDir.mkdirs()

        // Write an index.md from the template's header/introduction (everything before the first recipe)
        val indexContent = sections.firstSectionPrefix.trimEnd() + "\n"
        File(outDir, "index.md").writeText(indexContent)

        val tokenRegex = Regex("\\{\\{([^}]+)}}")
        // Only process sections that contain a placeholder token
        val recipeSections = sections.sections.filter { tokenRegex.containsMatchIn(it.content) }

        // For each recipe section, render a page replacing the token with the snippet code fence
        recipeSections.forEach { section ->
            val tokenMatch = tokenRegex.find(section.content)
            val tokenName = tokenMatch!!.groups[1]!!.value
            val snippet = snippets[tokenName]
            check(snippet != null) { "No snippet found for token $tokenName" }

            val codeFence = "```kotlin\n$snippet\n```"
            val body = section.content.replace("{{${tokenName}}}", codeFence)

            // Convert the section heading (H2 with optional Recipe N) into an H1 without numeric prefix
            val title = extractTitleFromHeading(section.heading)
            val bodyWithTitle = body.lines().toMutableList().apply {
                // Replace the first line (the H2) with H1 title
                if (isNotEmpty()) {
                    this[0] = "# $title"
                } else {
                    add("# $title")
                }
            }.joinToString("\n")

            // Derive slug from token name (remove leading recipe-\d+-)
            val slug = tokenName.replaceFirst(Regex("^recipe-\\d+-"), "")
            val filename = "$slug.md"
            File(outDir, filename).writeText(bodyWithTitle.trimEnd() + "\n")
        }
    }
}

abstract class VerifyRecipesDocsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sampleFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsDir: DirectoryProperty

    @TaskAction
    fun verify() {
        val snippets = extractRecipeSnippets(sampleFile.asFile.get().readText())
        val template = templateFile.asFile.get().readText()
        val sections = splitTemplateIntoSections(template)

        val docs = docsDir.asFile.get()
        check(docs.exists() && docs.isDirectory) { "Recipes docs directory does not exist: ${docs.absolutePath}. Run :konditional-observability:generateRecipesDocs." }

        // Verify index
        val expectedIndex = sections.firstSectionPrefix.trimEnd() + "\n"
        val actualIndexFile = File(docs, "index.md")
        check(actualIndexFile.exists()) { "Missing recipes index: ${actualIndexFile.absolutePath}. Run :konditional-observability:generateRecipesDocs." }
        val actualIndex = actualIndexFile.readText()
        check(expectedIndex == actualIndex) { "Recipes index is out of date. Run :konditional-observability:generateRecipesDocs." }

        val tokenRegex = Regex("\\{\\{([^}]+)}}")
        val recipeSections = sections.sections.filter { tokenRegex.containsMatchIn(it.content) }

        // Verify each recipe section file
        recipeSections.forEach { section ->
            val tokenMatch = tokenRegex.find(section.content)
            val tokenName = tokenMatch!!.groups[1]!!.value
            val snippet = snippets[tokenName]
            check(snippet != null) { "No snippet found for token $tokenName" }

            val codeFence = "```kotlin\n$snippet\n```"
            val body = section.content.replace("{{${tokenName}}}", codeFence)
            val title = extractTitleFromHeading(section.heading)
            val bodyWithTitle = body.lines().toMutableList().apply {
                if (isNotEmpty()) this[0] = "# $title" else add("# $title")
            }.joinToString("\n")

            val slug = tokenName.replaceFirst(Regex("^recipe-\\d+-"), "")
            val filename = "$slug.md"
            val actualFile = File(docs, filename)
            check(actualFile.exists()) { "Missing recipe page: ${actualFile.absolutePath}. Run :konditional-observability:generateRecipesDocs." }
            val actual = actualFile.readText()
            check(bodyWithTitle.trimEnd() + "\n" == actual) { "Recipes page out of date: ${actualFile.absolutePath}. Run :konditional-observability:generateRecipesDocs." }
        }
    }
}

private data class Section(val heading: String, val content: String)

private data class SectionsSplit(val firstSectionPrefix: String, val sections: List<Section>)

private fun splitTemplateIntoSections(template: String): SectionsSplit {
    val lines = template.lineSequence().toList()
    val sections = mutableListOf<Section>()

    val tokenRegex = Regex("\\{\\{([^}]+)}}")

    var i = 0
    // We'll collect all H2 blocks, but only treat blocks that contain a token placeholder as recipe sections.
    val blocks = mutableListOf<Pair<String, List<String>>>()

    val prefixBuilder = StringBuilder()
    while (i < lines.size) {
        val line = lines[i]
        if (line.trim().startsWith("## ")) {
            // start of a block
            val headingLine = line
            i++
            val blockLines = mutableListOf<String>()
            while (i < lines.size && !lines[i].trim().startsWith("## ")) {
                blockLines.add(lines[i])
                i++
            }
            blocks.add(Pair(headingLine, blockLines))
        } else {
            // part of prefix before the first H2
            prefixBuilder.append(line).append('\n')
            i++
        }
    }

    // Now, find the first block that contains a token placeholder; everything before that should be part of the prefix
    var firstRecipeIndex: Int? = null
    for ((idx, pair) in blocks.withIndex()) {
        val (_, blockLines) = pair
        val blockText = (listOf(pair.first) + blockLines).joinToString("\n")
        if (tokenRegex.containsMatchIn(blockText)) {
            firstRecipeIndex = idx
            break
        }
    }

    if (firstRecipeIndex == null) {
        // No recipe sections found; everything is prefix
        return SectionsSplit(prefixBuilder.toString(), emptyList())
    }

    // Append any blocks before the first recipe into the prefix
    for (j in 0 until firstRecipeIndex) {
        val (heading, blockLines) = blocks[j]
        prefixBuilder.append(heading).append('\n')
        for (ln in blockLines) prefixBuilder.append(ln).append('\n')
    }

    // For blocks from firstRecipeIndex onwards, treat only those containing token placeholder as recipe sections.
    for (j in firstRecipeIndex until blocks.size) {
        val (heading, blockLines) = blocks[j]
        val blockText = (listOf(heading) + blockLines).joinToString("\n")
        if (tokenRegex.containsMatchIn(blockText)) {
            // Trim trailing separator lines (---) if present
            val trimmedLines = blockLines.toMutableList()
            while (trimmedLines.isNotEmpty() && trimmedLines.last().trim() == "---") {
                trimmedLines.removeAt(trimmedLines.size - 1)
            }
            val content = (listOf(heading) + trimmedLines).joinToString("\n")
            sections.add(Section(heading, content))
        } else {
            // Non-recipe block after recipes; append to prefix (so Next Steps end up in index)
            prefixBuilder.append(heading).append('\n')
            for (ln in blockLines) prefixBuilder.append(ln).append('\n')
        }
    }

    return SectionsSplit(prefixBuilder.toString(), sections)
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

private fun extractTitleFromHeading(headingLine: String): String {
    // headingLine expected like: "## Recipe 1: Typed Variants Instead of Boolean Explosion" or "## Typed Variants Instead of Boolean Explosion"
    val trimmed = headingLine.trim()
    val afterHash = if (trimmed.startsWith("##")) trimmed.removePrefix("##").trim() else trimmed
    // Remove optional leading "Recipe <num>: " if present
    val title = afterHash.replaceFirst(Regex("^Recipe\\s+\\d+:\\s*"), "")
    return title
}
