# First-Time Publishing Setup Checklist

Complete this checklist before your first release to Maven Central.

## Prerequisites

### 1. Sonatype OSSRH Account Setup

- [ ] Create account at https://issues.sonatype.org
- [ ] Create a ticket for namespace claim:
  - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
  - Issue Type: New Project
  - Group Id: `io.amichne`
  - Project URL: https://github.com/amichne/konditional
  - SCM URL: https://github.com/amichne/konditional.git
- [ ] Wait for approval (typically 1-2 business days)
- [ ] Save your username and password

**Resources**:
- [OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Ticket Template](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134)

### 2. GPG Key Setup

Generate a GPG key for signing artifacts:

```bash
# Install GPG (if not already installed)
# macOS: brew install gnupg
# Linux: apt-get install gnupg

# Generate key
gpg --full-generate-key

# Follow prompts:
# - Choose: RSA and RSA
# - Key size: 4096 bits
# - Expiration: 0 (no expiration, or choose based on your policy)
# - Real name: Your name
# - Email: Your email
# - Passphrase: Choose a strong passphrase
```

- [ ] GPG key generated
- [ ] Record your key ID: `gpg --list-secret-keys --keyid-format=long`
- [ ] Note the 8-character short ID (last 8 chars of the long ID)

Publish your key to a keyserver:

```bash
# Find your key ID
gpg --list-secret-keys --keyid-format=long

# Publish to keyserver (replace YOUR_KEY_ID)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Verify it's published
gpg --keyserver keyserver.ubuntu.com --recv-keys YOUR_KEY_ID
```

- [ ] Key published to keyserver.ubuntu.com

Export the private key for CI:

```bash
# Export in ASCII armor format (replace YOUR_KEY_ID)
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# Display the key (you'll copy this value)
cat private-key.asc
```

- [ ] Private key exported to `private-key.asc`
- [ ] **IMPORTANT**: Keep this file secure, delete after setup

### 3. Local Gradle Configuration

Create or update `~/.gradle/gradle.properties`:

```properties
ossrhUsername=YOUR_SONATYPE_USERNAME
ossrhPassword=YOUR_SONATYPE_PASSWORD

signing.keyId=YOUR_KEY_ID_LAST_8_CHARS
signing.password=YOUR_GPG_PASSPHRASE
signing.secretKeyRingFile=/Users/yourname/.gnupg/secring.gpg

githubActor=YOUR_GITHUB_USERNAME
githubToken=YOUR_GITHUB_TOKEN
```

**Notes**:
- Replace `YOUR_*` placeholders with actual values
- Update the path in `signing.secretKeyRingFile` to your home directory
- The GitHub token is optional for local publishing

- [ ] Local gradle.properties configured
- [ ] File permissions set to 600: `chmod 600 ~/.gradle/gradle.properties`

### 4. GitHub Repository Setup

#### Configure Secrets

Navigate to: `Repository Settings → Secrets and variables → Actions → New repository secret`

Add these secrets:

- [ ] `OSSRH_USERNAME` - Your Sonatype username
- [ ] `OSSRH_PASSWORD` - Your Sonatype password
- [ ] `SIGNING_KEY` - Contents of `private-key.asc` (entire file, including BEGIN/END lines)
- [ ] `SIGNING_PASSWORD` - Your GPG passphrase

**Verification**:
- Secrets are encrypted and cannot be viewed after creation
- `GITHUB_TOKEN` is automatically provided, no need to create it

#### Enable Actions

- [ ] GitHub Actions enabled for repository
- [ ] Workflow permissions set to "Read and write permissions"
  - Settings → Actions → General → Workflow permissions

## Verification Tests

### Local Publishing Test

Test that everything is configured correctly:

```bash
# 1. Build and generate POM
./gradlew generatePomFileForMavenPublication

# 2. Verify POM contents
cat build/publications/maven/pom-default.xml

# 3. Publish to local Maven repository
./gradlew publishToMavenLocal

# 4. Verify artifacts exist
ls -lh ~/.m2/repository/io/amichne/konditional/0.0.1/
```

Expected artifacts:
- `konditional-0.0.1.jar` - Main library
- `konditional-0.0.1-sources.jar` - Source code
- `konditional-0.0.1-javadoc.jar` - Documentation
- `konditional-0.0.1.pom` - Maven metadata
- `konditional-0.0.1.module` - Gradle metadata

- [ ] All artifacts generated successfully
- [ ] POM contains correct metadata
- [ ] No errors in build output

### CI/CD Pipeline Test

Test GitHub Actions locally (optional):

```bash
# Run tests locally
./gradlew clean test

# Verify all tests pass
./gradlew build --stacktrace
```

- [ ] Tests pass locally
- [ ] Build completes without errors

Push to a feature branch and verify CI:

```bash
# Create a test branch
git checkout -b test-publishing-setup

# Make a trivial change and commit
git commit --allow-empty -m "Test publishing setup"

# Push and watch Actions
git push origin test-publishing-setup
```

- [ ] CI workflow runs successfully
- [ ] Tests pass on all platforms
- [ ] No configuration errors

## Security Checklist

- [ ] Private key file (`private-key.asc`) deleted after GitHub Secrets setup
- [ ] `~/.gradle/gradle.properties` has 600 permissions
- [ ] No credentials committed to git
- [ ] `.gitignore` includes:
  - `*.asc`
  - `*.gpg`
  - `private-key.*`
  - `local.properties`

## Post-Setup

Once everything is verified:

```bash
# Delete the test branch
git branch -D test-publishing-setup
git push origin --delete test-publishing-setup

# Delete the exported private key
rm private-key.asc
```

- [ ] Test branch cleaned up
- [ ] Private key file deleted
- [ ] Setup documented for team

## Ready to Release!

With all items checked, you're ready to publish your first release:

```bash
# 1. Ensure you're on main branch
git checkout main
git pull

# 2. Run the release script
./scripts/prepare-release.sh

# 3. Monitor the release
# https://github.com/amichne/konditional/actions
```

## Troubleshooting

### "Could not find secring.gpg"

Modern GPG versions don't create `secring.gpg`. The build configuration handles this automatically by using environment variables in CI. For local publishing, GPG agent is used automatically.

**Solution**: No action needed if you set up the GitHub Secrets correctly.

### "Unauthorized" when publishing

**Check**:
1. Verify credentials in `~/.gradle/gradle.properties`
2. Test Sonatype credentials at https://s01.oss.sonatype.org/
3. Ensure namespace ticket is approved

### "Signature validation failed"

**Check**:
1. Key is published: `gpg --keyserver keyserver.ubuntu.com --recv-keys YOUR_KEY_ID`
2. `SIGNING_KEY` secret contains full ASCII armored key (including BEGIN/END lines)
3. `SIGNING_PASSWORD` matches your GPG passphrase

### CI fails but local works

**Check**:
1. All GitHub Secrets are set correctly
2. Secrets don't have extra whitespace
3. Review workflow logs for specific error messages

## Support

- OSSRH Guide: https://central.sonatype.org/publish/publish-guide/
- GPG Guide: https://central.sonatype.org/publish/requirements/gpg/
- GitHub Actions Docs: https://docs.github.com/en/actions
- Project-specific docs: See `PUBLISHING.md` in repository root
