package io.amichne.konditional.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class KonditionalCoreApiBoundaryTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:Input
    abstract val allowedPackagePrefixes: ListProperty<String>

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun checkApiBoundary() {
        val allowedPrefixes = allowedPackagePrefixes.get()
        val rootDir = projectDir.get().asFile
        val kotlinFiles = sourceDir.get().asFileTree.matching { include("**/*.kt") }.files

        val packageViolations =
            kotlinFiles.mapNotNull { file ->
                val pkg = findPackageOrNull(file)
                when {
                    pkg == null -> file.relativeTo(rootDir).path to "<missing package>"
                    else -> null
                }
            }

        val internalMarkerViolations =
            kotlinFiles.flatMap { file ->
                val pkg = findPackageOrNull(file)
                if (pkg == null) {
                    emptyList()
                } else {
                    val requiresInternalMarker = allowedPrefixes.none { prefix -> pkg.startsWith(prefix) }
                    if (requiresInternalMarker) {
                        findMissingInternalMarkerDeclarations(file, pkg, rootDir)
                    } else {
                        emptyList()
                    }
                }
            }

        val hasErrors = packageViolations.isNotEmpty() || internalMarkerViolations.isNotEmpty()
        require(!hasErrors) {
            buildString {
                if (packageViolations.isNotEmpty()) {
                    appendLine("Found Kotlin files with missing package declarations:")
                    packageViolations.forEach { (path, pkg) -> appendLine("  - $path (package $pkg)") }
                }
                if (internalMarkerViolations.isNotEmpty()) {
                    appendLine("Found Kotlin classes in prohibited packages missing @KonditionalInternalApi:")
                    internalMarkerViolations.forEach { violation ->
                        appendLine("  - ${violation.path} (package ${violation.pkg}, ${violation.declaration})")
                    }
                }
            }
        }
    }

    private fun findPackageOrNull(file: File): String? =
        file.useLines { lines ->
            lines.firstOrNull { it.trimStart().startsWith("package ") }
                ?.removePrefix("package")
                ?.trim()
        }

    private fun findMissingInternalMarkerDeclarations(
        file: File,
        pkg: String,
        rootDir: File,
    ): List<InternalMarkerViolation> {
        val violations = mutableListOf<InternalMarkerViolation>()
        val lines = file.readLines()
        var pendingInternalMarker = false
        var inBlockComment = false
        var braceDepth = 0

        for (index in lines.indices) {
            val line = lines[index]
            val trimmed = line.trim()

            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false
                }
                braceDepth = updateBraceDepth(braceDepth, line)
                continue
            }

            if (trimmed.startsWith("/*")) {
                inBlockComment = !trimmed.contains("*/")
                braceDepth = updateBraceDepth(braceDepth, line)
                continue
            }

            if (trimmed.startsWith("//") || trimmed.isEmpty()) {
                braceDepth = updateBraceDepth(braceDepth, line)
                continue
            }

            if (braceDepth == 0 && trimmed.startsWith("@")) {
                if (isInternalMarkerAnnotation(trimmed)) {
                    pendingInternalMarker = true
                }
                braceDepth = updateBraceDepth(braceDepth, line)
                continue
            }

            if (braceDepth == 0) {
                val declaration = parseClassDeclaration(trimmed)
                if (declaration != null) {
                    val isInternalVisibility =
                        declaration.visibility == "internal" || declaration.visibility == "private"
                    val hasInternalMarker = pendingInternalMarker || declaration.hasInternalMarker
                    if (!isInternalVisibility && !hasInternalMarker) {
                        violations.add(
                            InternalMarkerViolation(
                                path = file.relativeTo(rootDir).path,
                                pkg = pkg,
                                declaration = "${declaration.kind} ${declaration.name} (line ${index + 1})",
                            )
                        )
                    }
                    pendingInternalMarker = false
                } else {
                    pendingInternalMarker = false
                }
            }

            braceDepth = updateBraceDepth(braceDepth, line)
        }

        return violations.toList()
    }

    private fun updateBraceDepth(currentDepth: Int, line: String): Int {
        val opens = line.count { it == '{' }
        val closes = line.count { it == '}' }
        val nextDepth = currentDepth + opens - closes
        return if (nextDepth < 0) 0 else nextDepth
    }

    private fun parseClassDeclaration(line: String): ClassDeclaration? {
        val tokens = wordRegex.findAll(line).map { it.value }.toList()
        val keywordIndex = tokens.indexOfFirst { token ->
            token == "class" || token == "interface" || token == "object"
        }
        val name = if (keywordIndex >= 0) tokens.getOrNull(keywordIndex + 1) else null
        val visibility = tokens.firstOrNull { token -> token in visibilityTokens }
        val declarationKind = if (keywordIndex >= 0) tokens[keywordIndex] else null
        val hasInternalMarker = isInternalMarkerAnnotation(line)

        return if (keywordIndex >= 0 && name != null && declarationKind != null) {
            ClassDeclaration(
                name = name,
                kind = declarationKind,
                visibility = visibility,
                hasInternalMarker = hasInternalMarker,
            )
        } else {
            null
        }
    }

    private fun isInternalMarkerAnnotation(line: String): Boolean =
        !line.trimStart().startsWith("@file:") && internalMarkerRegex.containsMatchIn(line)

    private data class InternalMarkerViolation(
        val path: String,
        val pkg: String,
        val declaration: String,
    )

    private data class ClassDeclaration(
        val name: String,
        val kind: String,
        val visibility: String?,
        val hasInternalMarker: Boolean,
    )

    private companion object {
        val visibilityTokens = setOf("public", "internal", "private", "protected")
        val internalMarkerRegex =
            Regex("@(?:io\\.amichne\\.konditional\\.api\\.)?KonditionalInternalApi\\b")
        val wordRegex = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
