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

        val violations =
            kotlinFiles.mapNotNull { file ->
                val pkg = findPackageOrNull(file)
                when {
                    pkg == null -> file.relativeTo(rootDir).path to "<missing package>"
                    allowedPrefixes.none { prefix -> pkg.startsWith(prefix) } -> file.relativeTo(rootDir).path to pkg
                    else -> null
                }
            }

        require(violations.isEmpty()) {
            buildString {
                appendLine("Found Kotlin files outside the :konditional-core package allowlist:")
                violations.forEach { (path, pkg) -> appendLine("  - $path (package $pkg)") }
            }
        }
    }

    private fun findPackageOrNull(file: File): String? =
        file.useLines { lines ->
            lines.firstOrNull { it.trimStart().startsWith("package ") }
                ?.removePrefix("package")
                ?.trim()
        }
}

