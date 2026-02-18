---
title: Publishing releases
---

# Publishing releases

This guide explains the canonical publishing flow for Konditional.
Use `make publish` as the single entrypoint for local, snapshot, release,
and GitHub publication.

## Prerequisites

Set up credentials and signing before publishing.

### Maven Central credentials

Konditional publishes to Maven Central. You must configure one of these
credential pairs in `~/.gradle/gradle.properties`:

```properties
# Option 1
ossrhUsername=YOUR_TOKEN_USERNAME
ossrhPassword=YOUR_TOKEN_PASSWORD

# Option 2
mavenCentralUsername=YOUR_TOKEN_USERNAME
mavenCentralPassword=YOUR_TOKEN_PASSWORD
```

### GPG signing

Release and snapshot publication require signing. Configure signing in
`~/.gradle/gradle.properties`:

```properties
signing.gnupg.keyName=ABCD1234EFGH5678
signing.gnupg.executable=gpg
```

If you use legacy keyring signing, configure `signing.keyId`,
`signing.password`, and `signing.secretKeyRingFile`.

### GitHub Packages credentials

GitHub publication requires one of these credential options:

1. `gpr.user` and `gpr.key` in `~/.gradle/gradle.properties`.
2. `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables.

### Optional `fzf`

Install `fzf` if you want interactive fuzzy selection in `make publish`.
If `fzf` is unavailable, the flow falls back to a numbered prompt.

## Canonical publishing flow

Use this flow for all publication work.

### Interactive mode

Run the single on-rails entrypoint:

```bash
make publish
```

The flow prompts for:

1. Publish target: `local`, `snapshot`, `release`, or `github`.
2. Version action: bump mode and snapshot strategy.

### Non-interactive mode

Use `make publish-plan` for CI scripts or repeatable local commands:

```bash
make publish-plan PUBLISH_TARGET=local VERSION_CHOICE=none
```

Valid values:

- `PUBLISH_TARGET`: `local`, `snapshot`, `release`, `github`
- `VERSION_CHOICE`: `none`, `snapshot`, `patch`, `minor`, `major`,
  `patch-snapshot`, `minor-snapshot`, `major-snapshot`

### Version choices

The publish flow supports these version actions:

- `none`: keep the current version unchanged.
- `snapshot`: keep the same base version and enforce `-SNAPSHOT`.
- `patch|minor|major`: bump semantic version without `-SNAPSHOT`.
- `*-snapshot`: bump semantic version and append `-SNAPSHOT`.

## Makefile publish nodes

`make publish` orchestrates these explicit sub-nodes:

- `publish-version-*`: version selection and update.
- `publish-validate-*`: target-aware validation.
- `publish-run-*`: target publish execution.

You can run these directly when you need fine-grained control.

### Target-specific aliases

These aliases run validation and then publish:

```bash
make publish-local
make publish-snapshot
make publish-release
make publish-github
```

## Validation behavior

Validation is target aware:

- `local`: no signing or remote credentials required.
- `snapshot` and `release`: signing and Maven Central credentials required.
- `github`: GitHub Packages credentials required.

Validation also confirms publishable modules and Gradle publish task
resolution through Makefile nodes.

Run validation directly:

```bash
make publish-validate-local
make publish-validate-snapshot
make publish-validate-release
make publish-validate-github
```

## Legacy script compatibility

These scripts remain available and delegate to the Makefile rails:

- `scripts/publish.sh`
- `scripts/prepare-release.sh`
- `scripts/validate-publish.sh`
- `scripts/bump-version.sh`

Prefer `make publish` for day-to-day usage.

## Published modules

The following modules publish artifacts:

| Module                  | Artifact ID                     |
|-------------------------|---------------------------------|
| `konditional-core`      | `konditional-core`              |
| `konditional-runtime`   | `konditional-runtime`           |
| `konditional-serialization` | `konditional-serialization` |
| `konditional-observability` | `konditional-observability` |
| `konditional-otel`      | `konditional-opentelemetry`     |
| `kontracts`             | `konditional-kontracts`         |
| `openapi`               | `konditional-openapi`           |
| `openfeature`           | `konditional-openfeature`       |

## Troubleshooting

### Validation fails for credentials

If validation fails for credentials, verify that the required keys exist in
`~/.gradle/gradle.properties` or environment variables for your chosen target.

### Snapshot or release blocked by version format

Use a compatible version choice:

- Snapshot publish requires a `-SNAPSHOT` version.
- Release publish requires a non-`-SNAPSHOT` version.

### GPG signing errors

If signing fails, verify key availability:

```bash
gpg --list-secret-keys
```

Then verify your configured key ID in `signing.gnupg.keyName`.
