package io.amichne.konditional.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations
import javax.inject.Inject

private fun validateCiMode(ciMode: String): String {
    require(ciMode == "warn" || ciMode == "strict") {
        "ciMode must be one of: warn, strict"
    }
    return ciMode
}

private fun DefaultTask.validatorArgs(
    validatorScriptPath: String,
    repoRootPath: String,
    linksPath: String,
    claimsPath: String,
    reportPath: String,
    signaturesDirValue: String,
    requireTestsValue: Boolean,
    ciModeValue: String,
): List<String> {
    val validatedCiMode = validateCiMode(ciModeValue)

    val args = mutableListOf(
        "python3",
        validatorScriptPath,
        "--repo-root",
        repoRootPath,
        "--links-file",
        linksPath,
        "--claims-file",
        claimsPath,
        "--report-out",
        reportPath,
        "--signatures-dir",
        signaturesDirValue,
        "--ci-mode",
        validatedCiMode,
    )

    if (requireTestsValue) {
        args.add("--require-tests")
    } else {
        args.add("--no-require-tests")
    }

    return args
}

abstract class GenerateJourneyClaimsReportTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val validatorScript: RegularFileProperty

    @get:Internal
    abstract val repoRoot: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linksFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val claimsFile: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val signaturesDir: org.gradle.api.provider.Property<String>

    @get:Input
    abstract val requireTests: org.gradle.api.provider.Property<Boolean>

    @get:Input
    abstract val ciMode: org.gradle.api.provider.Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val repoRootFile = repoRoot.get().asFile
        val args =
            validatorArgs(
                validatorScriptPath = validatorScript.get().asFile.invariantSeparatorsPath,
                repoRootPath = repoRootFile.invariantSeparatorsPath,
                linksPath = linksFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                claimsPath = claimsFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                reportPath = reportFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                signaturesDirValue = signaturesDir.get(),
                requireTestsValue = requireTests.get(),
                ciModeValue = ciMode.get(),
            )

        reportFile.get().asFile.parentFile.mkdirs()
        execOperations.exec {
            workingDir = repoRootFile
            commandLine(args)
        }
    }
}

abstract class VerifyJourneyClaimsDocsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val validatorScript: RegularFileProperty

    @get:Internal
    abstract val repoRoot: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val linksFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val claimsFile: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val signaturesDir: org.gradle.api.provider.Property<String>

    @get:Input
    abstract val requireTests: org.gradle.api.provider.Property<Boolean>

    @get:Input
    abstract val ciMode: org.gradle.api.provider.Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val repoRootFile = repoRoot.get().asFile
        val args =
            validatorArgs(
                validatorScriptPath = validatorScript.get().asFile.invariantSeparatorsPath,
                repoRootPath = repoRootFile.invariantSeparatorsPath,
                linksPath = linksFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                claimsPath = claimsFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                reportPath = reportFile.get().asFile.relativeTo(repoRootFile).invariantSeparatorsPath,
                signaturesDirValue = signaturesDir.get(),
                requireTestsValue = requireTests.get(),
                ciModeValue = ciMode.get(),
            )

        reportFile.get().asFile.parentFile.mkdirs()
        execOperations.exec {
            workingDir = repoRootFile
            commandLine(args)
        }
    }
}
