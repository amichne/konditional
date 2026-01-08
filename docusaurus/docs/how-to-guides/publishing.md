---
title: Publishing Releases
---

# Publishing Releases

This guide is for maintainers who cut releases of Konditional.

## JReleaser release metadata

**Guarantee**: JReleaser uses the same project metadata as Maven publishing (name, description, website, license, author).
**Mechanism**: `jreleaser.project` is populated from `gradle.properties` in `build.gradle.kts`.
**Boundary**: Incorrect `POM_*` values result in incorrect release metadata; JReleaser does not validate content semantics.

## GitHub release flow

**Guarantee**: `jreleaserFullRelease` creates a GitHub release for the current project version when credentials are present.
**Mechanism**: `jreleaser.release.github` points at the repository owner and project name; the Gradle version becomes the tag.
**Boundary**: Missing credentials or missing tags stop the release step; no GitHub release is created.

Steps:
1. Update `VERSION` in `gradle.properties` to a release (non-SNAPSHOT) version.
2. Provide GitHub credentials via your JReleaser configuration (for example, `~/.jreleaser/config.properties`).
3. Verify configuration with `./gradlew jreleaserConfig`.
4. Run `./gradlew jreleaserFullRelease` to create the release.

## Maven Central publishing (current pipeline)

**Guarantee**: Maven artifacts are produced and signed by Gradle when signing credentials are present.
**Mechanism**: `configureKonditionalPublishing` and `io.github.gradle-nexus.publish-plugin` handle publishing and staging.
**Boundary**: JReleaser does not replace Sonatype publishing unless the deploy flow is explicitly configured.

Steps:
1. Run `make publish-release` to publish to Sonatype staging.
2. Release the staging repository in Sonatype when validation completes.
