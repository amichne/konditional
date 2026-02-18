plugins {
    id("io.github.simonhauck.release") version "1.5.1"
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
