# Build, docs, and release

## Local builds

Use the narrowest command that proves your change.

- `make build` compiles all Gradle modules.
- `make test` runs the Kotlin test suite.
- `make detekt` runs static analysis.
- `make check` runs the static analysis target and tests.

## Documentation workflow

Docs live under `docs/` and are built with Zensical using `zensical.toml`.

- `make docs-install` creates `docs/venv` and installs the docs toolchain.
- `make docs-build` builds the static site into `site/`.
- `make docs-serve` runs the local docs server.
- `make docs-clean` removes generated docs output.

## Publishing workflow

To preview a patch release, run this command from the repository root.

```bash
make publish-plan PUBLISH_TARGET=release VERSION_CHOICE=patch
```

Targets are `local`, `snapshot`, `release`, and `github`.

- `make validate-publish` checks release prerequisites.
- `make publish PUBLISH_TARGET=release VERSION_CHOICE=patch` applies the version action and publishes. It asks for confirmation in a terminal and runs unattended with closed stdin.
- `make publish-local`, `make publish-snapshot`, `make publish-release`, and `make publish-github` publish the current version to the selected destination.

## GitHub Actions

- `CI` runs tests and builds the docs site.
- `Documentation` builds and deploys the Zensical site on pushes to `main` or `master`.
- `Publish Snapshot` validates and publishes snapshot artifacts.
- `Release` validates, publishes, and attaches release artifacts.
