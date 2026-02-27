---
name: konditional-maven-central-release-fastpath
description: Release Konditional to Maven Central through a fast, release-only workflow with minimal friction. Use when handling GitHub release pipelines or local release runs where snapshots are out of scope, slower enterprise-style gates are optional, and quick preflight failure is preferred.
---

# Konditional Maven Central Release Fastpath

## Overview

Use this skill to run Maven Central releases for Konditional with a fast-path workflow that is intentionally lighter than enterprise release gates. Keep release quality guardrails that prevent common failures, but avoid slow checks unless a full validation run is needed.

## Operating Policy

1. Use release target only: `PUBLISH_TARGET=release`.
2. Allow version choices `none|patch|minor|major` only.
3. Treat snapshots as out of scope for this skill.
4. Default to fast preflight (`VALIDATE_PUBLISH_SKIP_SMOKE=1`) to catch credentials, signing, and version problems early.
5. Escalate to full validation only after a failure or when explicitly requested.

## Quick Start

From the repository root:

```bash
bash ".agents/skills/konditional-maven-central-release-fastpath/scripts/release_fastpath.sh" --version-choice patch
```

For tag-driven releases where version is already set:

```bash
bash ".agents/skills/konditional-maven-central-release-fastpath/scripts/release_fastpath.sh" --version-choice none
```

Run full preflight (slower, stricter):

```bash
bash ".agents/skills/konditional-maven-central-release-fastpath/scripts/release_fastpath.sh" --version-choice patch --full-validation
```

## Workflow

1. Confirm you are in the Konditional repo root.
2. Run `scripts/release_fastpath.sh` with an allowed `--version-choice`.
3. Let the script run preflight validation and then `make publish-plan PUBLISH_TARGET=release`.
4. If publish fails, rerun with `--full-validation` and inspect the failing step before changing pipeline shape.

## GitHub Actions Guidance

Prefer a lightweight release job that:

1. Uses `workflow_dispatch` with `version-choice` input (`none|patch|minor|major`).
2. Imports GPG key material once.
3. Invokes this skill script rather than duplicating release logic in YAML.
4. Skips snapshot lanes entirely.

Use [`references/github-release-fastpath.md`](references/github-release-fastpath.md) for a minimal workflow template and required secrets.

## Resources

- `scripts/release_fastpath.sh`: fast release runner for local or CI execution.
- `references/github-release-fastpath.md`: minimal GitHub Actions template and operator notes.
