# Contributing to Konditional

Use JDK 27 to build the repository. Kotlin 2.4.21 compiles the libraries for JVM 26.

## Set up the checkout

1. Clone the repository.
2. Run `./gradlew -v` to see which JDK Gradle uses.
3. Run `make build`.

## Change a module

- Keep identifiers, contexts, and result types in `konditional-types`. This module is independent of the engine and JSON modules.
- Keep evaluation and namespace state in `konditional-engine`.
- Parse external snapshots in `konditional-json`. If a snapshot fails to decode, keep the previous namespace state.

See [the module map](docs/reference/module-dependency-map.mdx) for dependencies.

## Prepare a pull request

1. Add tests for each behavior you change. For concurrent updates, cover whole-snapshot reads. For new inputs, cover accepted values and typed failures.
2. If you change a public API, update its KDoc and user guide.
3. Run `make test` for the Kotlin suite. Use `make check` for the repository checks.
4. For site changes, use Node.js 22 LTS and npm. Run `make docs-serve` to preview the MDX pages and `make docs-build` to check the site. Edit `docs/docs.json` for navigation changes.
5. Keep the pull request focused on one change. Use a Conventional Commit title such as `docs: correct the install guide`.
6. Review the impact on `CHANGELOG.md`. Request review after CI passes.

## Update the glossary

Edit `docs/appendix/glossary.json` to change a term, its definition, or its source. From the repository root, regenerate the reader table with `jq`.

```bash
jq -r '"---\ntitle: \"Glossary\"\n---\n\n| Term | Meaning |\n| --- | --- |", (.entries[] | "| \(.term) | \(.definition) |")' \
    docs/appendix/glossary.json > docs/appendix/glossary.mdx
```

## Report a problem

For bugs and feature requests, open a [GitHub issue](https://github.com/amichne/konditional/issues).

For a security report, contact the maintainer privately by [email](mailto:austin@michne.com).
