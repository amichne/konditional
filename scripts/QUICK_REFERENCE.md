# Publishing Quick Reference

Quick reference for common publishing tasks.

## Release a New Version

```bash
# 1. Bump version (patch: 0.0.1 → 0.0.2)
./scripts/bump-version.sh patch

# 2. Commit version bump
git add gradle.properties
git commit -m "Bump version to $(grep VERSION= gradle.properties | cut -d= -f2)"
git push origin main

# 3. Create and push release tag
./scripts/prepare-release.sh
```

## Version Bumping

```bash
# Patch release (0.0.1 → 0.0.2)
./scripts/bump-version.sh patch

# Minor release (0.0.1 → 0.1.0)
./scripts/bump-version.sh minor

# Major release (0.0.1 → 1.0.0)
./scripts/bump-version.sh major

# Snapshot version (0.0.1 → 0.0.2-SNAPSHOT)
./scripts/bump-version.sh patch --snapshot
```

## Publish Snapshot

**Via GitHub Actions** (recommended):
1. Go to: `Actions → Publish Snapshot`
2. Click "Run workflow"
3. Enter version: `0.0.2-SNAPSHOT`
4. Run

**Locally**:
```bash
# Update gradle.properties to snapshot version first
./gradlew publishToSonatype
```

## Test Publishing Locally

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Verify artifacts
ls ~/.m2/repository/io/amichne/konditional/

# Test in another project
# Add mavenLocal() to repositories in test project
```

## Check Build Status

```bash
# Run all tests
./gradlew test

# Build without publishing
./gradlew build

# Verify POM generation
./gradlew generatePomFileForMavenPublication
cat build/publications/maven/pom-default.xml
```

## Useful URLs

- **GitHub Actions**: https://github.com/amichne/konditional/actions
- **GitHub Releases**: https://github.com/amichne/konditional/releases
- **Maven Central Search**: https://central.sonatype.com/artifact/io.amichne/konditional
- **Sonatype Repository Manager**: https://s01.oss.sonatype.org/
- **GitHub Packages**: https://github.com/amichne/konditional/packages

## Gradle Tasks

```bash
# List all publishing tasks
./gradlew tasks --group publishing

# Build and sign
./gradlew build signMavenPublication

# Publish to Maven Central
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository

# Publish to GitHub Packages
./gradlew publishMavenPublicationToGitHubPackagesRepository

# Prepare release (validation task)
./gradlew prepareRelease
```

## Rollback a Release

If you need to rollback:

```bash
# Delete the git tag locally
git tag -d v0.0.2

# Delete the tag from remote
git push origin :refs/tags/v0.0.2

# Cancel any running GitHub Actions workflows
# Go to Actions → Click on running workflow → Cancel

# Note: Maven Central releases are immutable
# If already published to Maven Central, you must:
# 1. Publish a new version with the fix
# 2. Mark the bad version as deprecated in documentation
```

## First-Time Release Checklist

Before your first release to Maven Central:

- [ ] Sonatype OSSRH account created
- [ ] Namespace claim ticket approved (io.amichne)
- [ ] GPG key generated and published to keyserver
- [ ] GitHub Secrets configured:
  - [ ] `OSSRH_USERNAME`
  - [ ] `OSSRH_PASSWORD`
  - [ ] `SIGNING_KEY`
  - [ ] `SIGNING_PASSWORD`
- [ ] Local testing completed (`publishToMavenLocal`)
- [ ] CI/CD passing on main branch

## Post-Release Tasks

After each release:

```bash
# Bump to next snapshot version
./scripts/bump-version.sh patch --snapshot

# Commit
git add gradle.properties
git commit -m "Prepare next development iteration"
git push origin main
```

## Troubleshooting

**"Version already exists"**
```bash
# Maven Central versions are immutable
# Bump to a new version
./scripts/bump-version.sh patch
```

**"Unauthorized"**
```bash
# Check credentials in ~/.gradle/gradle.properties
# Verify GitHub Secrets in repository settings
```

**"Signature validation failed"**
```bash
# Verify GPG key is published
gpg --keyserver keyserver.ubuntu.com --recv-keys YOUR_KEY_ID

# Check SIGNING_KEY secret contains full ASCII armored key
```

## Documentation

For detailed information, see:
- `PUBLISHING.md` - Complete publishing guide
- `ai/changelog/publishing-infrastructure.md` - Implementation details
