import io.amichne.konditional.gradle.GenerateJourneyClaimsReportTask
import io.amichne.konditional.gradle.KonditionalJourneyClaimsDocsExtension
import io.amichne.konditional.gradle.VerifyJourneyClaimsDocsTask

val extension =
    extensions.create<KonditionalJourneyClaimsDocsExtension>("konditionalJourneyClaimsDocs")

val repoRootDir = rootProject.layout.projectDirectory

extension.validatorScript.convention(
    repoRootDir.file(
        ".agents/skills/value-journey-signature-linker/scripts/validate_journey_signature_links.py",
    ),
)
extension.linksFile.convention(repoRootDir.file("docs/value-journeys/journey-signature-links.json"))
extension.claimsFile.convention(repoRootDir.file("docs/value-journeys/journey-claims.json"))
extension.generatedReportFile.convention(
    repoRootDir.file("docs/value-journeys/journey-claims-report.json"),
)
extension.verifyReportFile.convention(
    layout.buildDirectory.file("reports/journey-claims-report-ci.json"),
)
extension.signaturesDir.convention(".signatures")
extension.requireTests.convention(true)
extension.ciMode.convention(
    providers.gradleProperty("journeyClaimsCiMode").orElse("warn"),
)

val generateJourneyClaimsReport =
    tasks.register<GenerateJourneyClaimsReportTask>("generateJourneyClaimsReport") {
        group = "documentation"
        description = "Generate deterministic journey claims evidence report."

        repoRoot.set(repoRootDir)
        validatorScript.set(extension.validatorScript)
        linksFile.set(extension.linksFile)
        claimsFile.set(extension.claimsFile)
        reportFile.set(extension.generatedReportFile)
        signaturesDir.set(extension.signaturesDir)
        requireTests.set(extension.requireTests)
        ciMode.set(extension.ciMode)
    }

val verifyJourneyClaimsDocs =
    tasks.register<VerifyJourneyClaimsDocsTask>("verifyJourneyClaimsDocs") {
        group = "verification"
        description = "Verify journey claims evidence and documentation references."

        repoRoot.set(repoRootDir)
        validatorScript.set(extension.validatorScript)
        linksFile.set(extension.linksFile)
        claimsFile.set(extension.claimsFile)
        reportFile.set(extension.verifyReportFile)
        signaturesDir.set(extension.signaturesDir)
        requireTests.set(extension.requireTests)
        ciMode.set(extension.ciMode)
    }

afterEvaluate {
    require(extension.validatorScript.isPresent) {
        "konditionalJourneyClaimsDocs.validatorScript must be set"
    }
    require(extension.linksFile.isPresent) {
        "konditionalJourneyClaimsDocs.linksFile must be set"
    }
    require(extension.claimsFile.isPresent) {
        "konditionalJourneyClaimsDocs.claimsFile must be set"
    }
    require(extension.generatedReportFile.isPresent) {
        "konditionalJourneyClaimsDocs.generatedReportFile must be set"
    }
    require(extension.verifyReportFile.isPresent) {
        "konditionalJourneyClaimsDocs.verifyReportFile must be set"
    }
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(verifyJourneyClaimsDocs)
}

tasks.matching { it.name == "assemble" }.configureEach {
    dependsOn(generateJourneyClaimsReport)
}
