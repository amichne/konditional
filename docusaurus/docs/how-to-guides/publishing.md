---
title: Publishing releases
---

# Publishing releases

Use this page as the canonical release workflow for local, snapshot, release,
and GitHub publication.

## Read this page when

- You need to publish any Konditional artifact.
- You need a repeatable target-specific validation and publish sequence.
- You need to verify credentials and signing before release.

## Deterministic steps

1. Select target and version strategy.

| Target | Typical use | Version expectation |
| --- | --- | --- |
| `local` | Developer verification | Any |
| `snapshot` | Pre-release distribution | Must end with `-SNAPSHOT` |
| `release` | Maven Central release | Must not end with `-SNAPSHOT` |
| `github` | GitHub Packages distribution | Any, based on policy |

2. Confirm credentials and signing.

- Maven Central: `ossrhUsername`/`ossrhPassword` or
  `mavenCentralUsername`/`mavenCentralPassword` in
  `~/.gradle/gradle.properties`.
- Signing: `signing.gnupg.keyName` (or legacy signing properties).
- GitHub Packages: `gpr.user`/`gpr.key` or `GITHUB_ACTOR`/`GITHUB_TOKEN`.

3. Run the canonical orchestration entrypoint.

```bash
make publish
```

4. Run non-interactive planning when you need deterministic automation.

```bash
make publish-plan PUBLISH_TARGET=release VERSION_CHOICE=patch
```

5. Run target-specific validation and publication nodes if needed.

```bash
make publish-validate-local
make publish-validate-snapshot
make publish-validate-release
make publish-validate-github

make publish-run-local
make publish-run-snapshot
make publish-run-release
make publish-run-github
```

6. Verify publication outcome.

- Confirm Gradle publish tasks succeeded.
- Confirm artifacts appear in the target repository.
- Record version, target, and commit SHA in release notes.

## Release checklist

- [ ] Target and version strategy are explicit and compatible.
- [ ] Required credentials and signing keys are configured.
- [ ] Validation completed for the chosen target.
- [ ] Publish run completed without fallback or manual patching.
- [ ] Artifact visibility was confirmed after upload.

## Next steps

- [Handling failures](/how-to-guides/handling-failures)
- [Failure modes](/production-operations/failure-modes)
- [Refresh patterns](/production-operations/refresh-patterns)
