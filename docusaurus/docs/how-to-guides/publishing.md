---
title: Publishing Releases
---

# Publishing Releases

This guide is for maintainers who cut releases of Konditional to Maven Central.

## Prerequisites

### 1. Sonatype User Tokens

Konditional publishes to Maven Central via Sonatype OSSRH. You'll need user tokens for authentication.

#### Create User Token

1. Sign in to [Sonatype Central Portal](https://central.sonatype.com)
2. Navigate to your [User Token](https://central.sonatype.com/usertoken) page
3. Click "Generate User Token"
4. Copy the generated username and password
5. Add these to `~/.gradle/gradle.properties` as `ossrhUsername` and `ossrhPassword`

**Important:** Use user tokens, not your Sonatype JIRA password. Tokens are scoped specifically for publishing and can be rotated without changing your account password.

### 2. GPG Key Setup

Konditional uses GPG signing via `gpg-agent` (recommended) for artifact signing.

#### Generate GPG Key (if needed)

```bash
# Generate new GPG key
gpg --full-generate-key

# Select: RSA and RSA
# Key size: 4096 bits
# Expiration: 2 years (recommended)
# Real name: Your Full Name
# Email: your-email@example.com
```

#### Configure GPG Agent

Ensure `gpg-agent` is running and can cache your passphrase:

```bash
# Test GPG agent
echo "test" | gpg --clearsign

# Configure agent for longer cache (optional)
# Add to ~/.gnupg/gpg-agent.conf:
default-cache-ttl 3600
max-cache-ttl 7200
```

#### Publish GPG Key

```bash
# List your keys
gpg --list-secret-keys --keyid-format=long

# Example output:
# sec   rsa4096/ABCD1234EFGH5678 2024-01-08 [SC] [expires: 2026-01-07]
#       Full fingerprint here
# uid                 [ultimate] Your Name <your-email@example.com>

# Export public key (use the key ID after rsa4096/)
gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678

# Verify it was published
gpg --keyserver keys.openpgp.org --recv-keys ABCD1234EFGH5678
```

### 3. Configure `~/.gradle/gradle.properties`

Create or edit `~/.gradle/gradle.properties` with your credentials:

```properties
# Sonatype OSSRH Credentials (from user token)
ossrhUsername=YOUR_SONATYPE_TOKEN_USERNAME
ossrhPassword=YOUR_SONATYPE_TOKEN_PASSWORD

# GPG Signing with gpg-agent (recommended)
# Use the full 40-character fingerprint or last 16 characters
signing.gnupg.keyName=ABCD1234EFGH5678
signing.gnupg.executable=gpg
```

## Publishing Workflow

### Snapshot Publishing

Publish development snapshots to Sonatype snapshots repository:

```bash
# Ensure version ends with -SNAPSHOT in gradle.properties
# VERSION=0.1.0-SNAPSHOT

# Validate and publish
./scripts/publish.sh snapshot
```

Snapshots are automatically available at:

- Repository: `https://s01.oss.sonatype.org/content/repositories/snapshots/`
- Artifacts: `io.amichne:konditional-*:0.1.0-SNAPSHOT`

### Release Publishing

Publish stable releases to Maven Central:

```bash
# 1. Update VERSION in gradle.properties (remove -SNAPSHOT)
# VERSION=0.1.0

# 2. Validate everything is configured correctly
./scripts/validate-publish.sh

# 3. Publish to Sonatype staging
./scripts/publish.sh release

# 4. Login to Sonatype and release
# - Visit: https://s01.oss.sonatype.org
# - Navigate to: Staging Repositories
# - Find: io.amichne staging repository
# - Click: Close (wait for validation, 2-5 minutes)
# - Click: Release (publishes to Maven Central)

# 5. Wait for Maven Central propagation (10-30 minutes)
# - Verify: https://search.maven.org/search?q=g:io.amichne
```

### Local Testing

Test publishing to local Maven repository:

```bash
./scripts/publish.sh local

# Artifacts are published to ~/.m2/repository/
# Use in other projects with: mavenLocal()
```

### Signature Artifact Drift Gate

Konditional tracks generated signature artifacts in `.signatures/` and enforces drift checks in CI.

```bash
# Regenerate signatures after Kotlin/API changes
./scripts/generate-signatures.sh

# Verify no drift before pushing
./scripts/check-signatures-drift.sh
```

If drift exists, regenerate and commit `.signatures/` updates in the same change as the source edits.

## Gradle Tasks

The publishing setup provides these Gradle tasks:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Publish all modules to Sonatype
./gradlew publishAllPublicationsToSonatypeRepository

# Publish and automatically close/release staging repository
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository

# Generate POM files for inspection
./gradlew generatePomFileForMavenPublication
```

## Published Modules

The following modules are published to Maven Central:

| Module        | Artifact ID                 | Description                                |
|---------------|-----------------------------|--------------------------------------------|
| Core          | `konditional-core`          | Core feature flag evaluation engine        |
| Serialization | `konditional-serialization` | JSON serialization and configuration codec |
| Runtime       | `konditional-runtime`       | Runtime operations and snapshot loading    |
| Observability | `konditional-observability` | Shadow evaluation and A/B testing          |

## JReleaser release metadata

**Guarantee**: JReleaser uses the same project metadata as Maven publishing (name, description, website, license,
author).
**Mechanism**: `jreleaser.project` is populated from `gradle.properties` in `build.gradle.kts`.
**Boundary**: Incorrect `POM_*` values result in incorrect release metadata; JReleaser does not validate content
semantics.

## GitHub release flow

**Guarantee**: `jreleaserFullRelease` creates a GitHub release for the current project version when credentials are
present.
**Mechanism**: `jreleaser.release.github` points at the repository owner and project name; the Gradle version becomes
the tag.
**Boundary**: Missing credentials or missing tags stop the release step; no GitHub release is created.

Steps:

1. Update `VERSION` in `gradle.properties` to a release (non-SNAPSHOT) version.
2. Provide GitHub credentials via your JReleaser configuration (for example, `~/.jreleaser/config.properties`).
3. Verify configuration with `./gradlew jreleaserConfig`.
4. Run `./gradlew jreleaserFullRelease` to create the release.

## Troubleshooting

### GPG Agent Issues

```bash
# Restart gpg-agent
gpgconf --kill gpg-agent
gpgconf --launch gpg-agent

# Test signing
echo "test" | gpg --clearsign

# Check agent status
gpg-connect-agent 'getinfo version' /bye
```

### Signing Failures

If you get "gpg: signing failed: No secret key":

1. Verify key exists: `gpg --list-secret-keys`
2. Check `signing.gnupg.keyName` matches your key ID
3. Ensure key hasn't expired: `gpg --list-keys`

### Sonatype Upload Failures

If upload fails with 401 Unauthorized:

1. Verify credentials are correct in `~/.gradle/gradle.properties`
2. Use user token, not JIRA password
3. Ensure you have permission for `io.amichne` group ID

### POM Validation Errors

Common Sonatype validation errors:

- Missing Javadoc JAR: Ensure `java { withJavadocJar() }` in `build.gradle.kts`
- Missing Sources JAR: Ensure `java { withSourcesJar() }` in `build.gradle.kts`
- Invalid POM metadata: Check all `POM_*` properties in `gradle.properties`
- Missing GPG signature: Verify signing configuration
