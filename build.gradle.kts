plugins {
    id("io.github.simonhauck.release") version "1.5.1"
    id("konditional.journey-claims-docs")
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("version")
    .orElse(providers.gradleProperty("VERSION"))
    .get()

release {
    versionPropertyFile.set(layout.projectDirectory.file("gradle.properties"))
    releaseCommitMessage.set("chore(release): v{version}")
    postReleaseCommitMessage.set("chore(release): prepare v{version}")
    ignorePreReleaseDependencies.add("io.opentelemetry:opentelemetry-semconv")
}

konditionalJourneyClaimsDocs {
    ciMode.set(
        providers.gradleProperty("journeyClaimsCiMode").orElse("warn"),
    )
    signaturesDir.set(".signatures")
    requireTests.set(true)
}
