# Publishing Guide

This guide covers how to publish Konditional to Maven Central and GitHub Packages.

## Table of Contents

- [Prerequisites](#prerequisites)
- [First-Time Setup](#first-time-setup)
- [Publishing a Release](#publishing-a-release)
- [Publishing a Snapshot](#publishing-a-snapshot)
- [Local Testing](#local-testing)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### For Maintainers

1. **Sonatype OSSRH Account**
   - Create an account at https://issues.sonatype.org
   - Create a ticket to claim the `io.amichne` namespace
   - Wait for approval (usually 1-2 business days)

2. **GPG Key for Signing**
   - Install GPG: `brew install gnupg` (macOS) or `apt-get install gnupg` (Linux)
   - Generate a key: `gpg --full-generate-key`
   - Use RSA and RSA, 4096 bits, no expiration
   - Publish to keyserver: `gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID`

3. **GitHub Access**
   - Write access to the repository
   - Ability to create tags and releases

## First-Time Setup

### 1. Configure Sonatype Credentials

Add to `~/.gradle/gradle.properties`:

```properties
ossrhUsername=YOUR_SONATYPE_USERNAME
ossrhPassword=YOUR_SONATYPE_PASSWORD
signing.keyId=YOUR_KEY_ID_LAST_8_CHARS
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/yourname/.gnupg/secring.gpg
```

**Security Note**: Never commit these credentials to git!

### 2. Export GPG Key for CI

Export your private key:

```bash
# Export in ASCII armor format
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# Display the key (you'll copy this to GitHub Secrets)
cat private-key.asc
```

### 3. Configure GitHub Secrets

Navigate to: `Settings → Secrets and variables → Actions`

Add these secrets:

| Secret Name | Value | Description |
|------------|-------|-------------|
| `OSSRH_USERNAME` | Your Sonatype username | For Maven Central publishing |
| `OSSRH_PASSWORD` | Your Sonatype password | For Maven Central publishing |
| `SIGNING_KEY` | Contents of `private-key.asc` | GPG private key (entire file) |
| `SIGNING_PASSWORD` | Your GPG passphrase | For signing artifacts |

**Note**: `GITHUB_TOKEN` is automatically provided by GitHub Actions.

### 4. Verify Setup

Test locally:

```bash
# Build and sign
./gradlew build signMavenPublication

# Publish to Maven Local (does not require credentials)
./gradlew publishToMavenLocal

# Check the artifacts
ls ~/.m2/repository/io/amichne/konditional/
```

## Publishing a Release

### Quick Release Process

```bash
# 1. Ensure you're on main with latest changes
git checkout main
git pull

# 2. Bump version (choose: major, minor, or patch)
./scripts/bump-version.sh patch

# 3. Commit version bump
git add gradle.properties
git commit -m "Bump version to $(grep VERSION= gradle.properties | cut -d= -f2)"

# 4. Push to main
git push origin main

# 5. Prepare and tag release (runs tests, creates tag)
./scripts/prepare-release.sh

# 6. Monitor the release
# Check: https://github.com/amichne/konditional/actions
```

The automated workflow will:
1. ✅ Validate version matches tag
2. ✅ Run tests on multiple platforms
3. ✅ Publish to Maven Central
4. ✅ Publish to GitHub Packages
5. ✅ Create GitHub Release with artifacts
6. ✅ Generate changelog

### Manual Release Process

If you prefer to do it manually:

```bash
# 1. Update version in gradle.properties
# VERSION=0.0.2

# 2. Commit and tag
git add gradle.properties
git commit -m "Release 0.0.2"
git tag -a v0.0.2 -m "Release v0.0.2"

# 3. Push with tags
git push origin main --tags
```

### Post-Release Steps

After the release workflow completes:

```bash
# Bump to next snapshot version
./scripts/bump-version.sh patch --snapshot
git add gradle.properties
git commit -m "Prepare next development iteration"
git push origin main
```

## Publishing a Snapshot

Snapshots are useful for testing unreleased changes.

### Via GitHub Actions (Recommended)

1. Navigate to: `Actions → Publish Snapshot`
2. Click "Run workflow"
3. Enter snapshot version (e.g., `0.0.2-SNAPSHOT`)
4. Click "Run workflow"

### Locally

```bash
# Set snapshot version in gradle.properties
# VERSION=0.0.2-SNAPSHOT

# Publish
./gradlew publishToSonatype --no-daemon

# Or publish to GitHub Packages
./gradlew publishMavenPublicationToGitHubPackagesRepository
```

### Using Snapshots

Consumers need to add the snapshot repository:

```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("io.amichne:konditional:0.0.2-SNAPSHOT")
}
```

## Local Testing

### Publish to Maven Local

Test the publishing setup without uploading:

```bash
./gradlew publishToMavenLocal
```

Artifacts will be available at:
```
~/.m2/repository/io/amichne/konditional/
```

### Test in Another Project

Create a test project:

```kotlin
// settings.gradle.kts
repositories {
    mavenLocal()  // Use local repository
    mavenCentral()
}

// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional:0.0.1")
}
```

### Verify POM Generation

```bash
./gradlew generatePomFileForMavenPublication

# View the generated POM
cat build/publications/maven/pom-default.xml
```

## Troubleshooting

### "Could not find artifact"

**Symptom**: Consumers can't find the published artifact.

**Solutions**:
1. Maven Central sync takes up to 2 hours after release
2. Verify publication: https://central.sonatype.com/artifact/io.amichne/konditional
3. Check for typos in coordinates

### "Unauthorized" Error

**Symptom**: Publishing fails with 401 Unauthorized.

**Solutions**:
1. Verify credentials in `~/.gradle/gradle.properties`
2. Check GitHub Secrets are correctly configured
3. For Sonatype: ensure your account has access to `io.amichne` namespace

### "Signature validation failed"

**Symptom**: Maven Central rejects the upload due to bad signature.

**Solutions**:
1. Verify GPG key is published: `gpg --keyserver keyserver.ubuntu.com --recv-keys YOUR_KEY_ID`
2. Check `SIGNING_KEY` secret contains the full ASCII armored key
3. Verify `SIGNING_PASSWORD` is correct

### "Version already exists"

**Symptom**: Cannot publish because version is already in Maven Central.

**Solution**:
- Maven Central releases are immutable
- Bump to a new version: `./scripts/bump-version.sh patch`
- Never reuse version numbers

### Local Build Works, CI Fails

**Symptom**: Tests pass locally but fail in GitHub Actions.

**Solutions**:
1. Check Java version matches (both using Java 17)
2. Run with `--no-daemon` flag in CI
3. Enable `--stacktrace` to see full errors
4. Check for OS-specific path issues

### "Timeout" Errors

**Symptom**: Publishing times out during upload.

**Solutions**:
1. Timeouts are configured for 3 minutes in `build.gradle.kts`
2. Check network connectivity
3. Try again - Sonatype can be slow at peak times

## Maintenance

### Rotating GPG Keys

If you need to change GPG keys:

1. Generate new key
2. Publish to keyserver
3. Update GitHub Secrets
4. Update local `~/.gradle/gradle.properties`
5. Test with a snapshot release

### Updating Credentials

1. Update in `~/.gradle/gradle.properties` (local)
2. Update in GitHub Secrets (CI)
3. Test with `./gradlew publishToMavenLocal`

## Resources

- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Nexus Publish Plugin Docs](https://github.com/gradle-nexus/publish-plugin)
- [GitHub Packages Maven Docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [GPG Quickstart](https://central.sonatype.org/publish/requirements/gpg/)

## Support

If you encounter issues not covered here:

1. Check GitHub Actions logs: https://github.com/amichne/konditional/actions
2. Review Sonatype OSSRH tickets: https://issues.sonatype.org
3. Open an issue in the repository

## Version Strategy

- **Patch releases** (0.0.x): Bug fixes, documentation, non-breaking changes
- **Minor releases** (0.x.0): New features, backward-compatible API additions
- **Major releases** (x.0.0): Breaking changes, major refactors

During pre-1.0 development (0.x.x), breaking changes are permitted as documented in CLAUDE.md.
