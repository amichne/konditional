# GitHub Release Fastpath (Maven Central)

Use this template when you want a release-only workflow with minimal overhead.
This intentionally skips snapshot lanes and avoids large release orchestration.

## Release posture

1. Keep release path explicit and manual with `workflow_dispatch`.
2. Keep CI quality gates in regular PR/merge workflows.
3. Keep release workflow focused on publish concerns: version, signing, credentials, upload.
4. Use full validation only when fast path fails or when explicitly requested.

## Required secrets

- `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME`
- `ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD`
- `SIGNING_GPG_PRIVATE_KEY`
- `SIGNING_GPG_KEY_NAME`
- `SIGNING_GPG_PASSPHRASE` (optional)

## Minimal workflow template

```yaml
name: Release Fastpath

on:
  workflow_dispatch:
    inputs:
      version-choice:
        description: "Version action"
        type: choice
        required: true
        default: patch
        options: [none, patch, minor, major]

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: gradle

      - name: Import GPG key
        env:
          SIGNING_GPG_PRIVATE_KEY: ${{ secrets.SIGNING_GPG_PRIVATE_KEY }}
        run: |
          if ! printf '%s' "$SIGNING_GPG_PRIVATE_KEY" | gpg --batch --import; then
            printf '%s' "$SIGNING_GPG_PRIVATE_KEY" | base64 --decode | gpg --batch --import
          fi

      - name: Publish release
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD }}
          SIGNING_GPG_KEY_NAME: ${{ secrets.SIGNING_GPG_KEY_NAME }}
          SIGNING_GPG_PASSPHRASE: ${{ secrets.SIGNING_GPG_PASSPHRASE }}
        run: |
          bash ".agents/skills/konditional-maven-central-release-fastpath/scripts/release_fastpath.sh" \
            --version-choice "${{ inputs.version-choice }}"
```

## Full validation mode in CI

If a release fails and you need stricter diagnostics, rerun the script with:

```bash
bash ".agents/skills/konditional-maven-central-release-fastpath/scripts/release_fastpath.sh" \
  --version-choice patch \
  --full-validation
```

This re-enables the smoke validation checks that are skipped in fast mode.
