# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Commands (Gradle / Make)
This is a Kotlin/Gradle repo.

Hard rule: use `./gradlew` (or `make`, which wraps `./gradlew`). Do not call `gradle` directly.

### Build / test
- Build:
  - `make build`
  - `./gradlew build`
- Compile only:
  - `make compile`
  - `./gradlew compileKotlin`
- Run tests:
  - `make test`
  - `./gradlew test`

### Run a single test
JUnit 5 (Jupiter) is enabled.
- Single test class:
  - `./gradlew test --tests 'io.amichne.konditional.core.NamespaceFeatureDefinitionTest'`
- Single test method:
  - `./gradlew test --tests 'io.amichne.konditional.core.NamespaceFeatureDefinitionTest.someTestMethod'`
- Single test in a specific module:
  - `./gradlew :kontracts:test --tests 'io.amichne.kontracts.CustomTypeMappingTest'`

### Publish locally
- `make publish` (runs `publishToMavenLocal`)
- `./gradlew publishToMavenLocal`

### Documentation (MkDocs)
Docs are built with MkDocs from `docs/` using `mkdocs.yml`.
- Serve docs locally:
  - `make docs-serve`
- Build docs:
  - `make docs-build`
- Clean generated site output:
  - `make docs-clean`

### Linting
There is no dedicated linter/formatter task wired in (no ktlint/detekt/spotless Gradle plugins found). Use `./gradlew test` / `./gradlew build` as the primary validation.

## High-level architecture
### Modules
- Root module (`konditional`): the feature-flag library.
- `:kontracts`: a type-safe JSON Schema DSL used by `konditional` for safe JSON boundaries (custom structured flag values).
- `settings.gradle.kts` declares `:ktor-demo` / `:ktor-demo:demo-client`; in some checkouts these sources may be absent. If Gradle configuration or IDE sync fails due to missing demo sources, focus work on `konditional` + `kontracts`.

### Core runtime model (konditional)
At a high level, Konditional is:
- **Static flag definitions** (compile-time safety via delegated properties)
- **Namespace-scoped registries** (runtime isolation)
- **Deterministic evaluation** (rule precedence + SHA-256 bucketing)
- **Explicit JSON boundary** (parse into a `Configuration` snapshot; load atomically)

Key entry points and responsibilities:
- `io.amichne.konditional.core.Namespace`
  - Namespace = isolation boundary (each namespace delegates to its own `NamespaceRegistry`).
  - Primary way to define flags: `val myFlag by boolean<Context>(default = false) { rule(true) { ... } }`.
  - Property delegation eagerly creates + registers features at namespace init time (t0).
  - Delegation also updates the namespace registry definition (see `NamespaceRegistry.updateDefinition(...)`).
  - Exposes `allFeatures()` and namespace-scoped JSON helpers via `toJson()` / `fromJson(...)`.
- `io.amichne.konditional.core.registry.NamespaceRegistry` / `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry`
  - Holds the active `Configuration` snapshot in an `AtomicReference` for lock-free reads.
  - `load(...)` atomically swaps the snapshot and records a bounded rollback history.
  - Supports a namespace kill-switch (`disableAll()`), plus test-only overrides.
- `io.amichne.konditional.core.FlagDefinition`
  - The compiled/effective definition of a single flag: default + conditional values + salt + active state.
  - Evaluation iterates rules by descending specificity, then applies deterministic rollout bucketing.
- `io.amichne.konditional.rules.Rule` (+ `rules.evaluable.*`, `rules.versions.*`)
  - A `Rule` composes “base” targeting (locale/platform/version/axes) + an optional extension predicate.
  - Specificity is additive: base specificity + extension specificity.
- `io.amichne.konditional.core.evaluation.Bucketing`
  - Deterministic bucketing for rollouts: SHA-256 → bucket in [0, 10_000), threshold in basis points.
- Public API helpers live in `io.amichne.konditional.api.*`:
  - `Feature.evaluate(...)`, `evaluateWithReason(...)` for explainable decisions.
  - `Feature.evaluateWithShadow(...)` for migration/shadow comparisons between registries/configs.

### Serialization / remote config boundary
- `io.amichne.konditional.serialization.SnapshotSerializer`
  - Converts a full `Configuration` to/from JSON; supports patch application.
  - Deserialization returns `ParseResult` (no exceptions on parse failures).
- `io.amichne.konditional.serialization.NamespaceSnapshotSerializer`
  - Namespace-scoped JSON; on success, loads directly into the namespace.

Important invariant when working with JSON config:
- Namespaces must be initialized before deserializing/loading snapshots, so the registry knows which features exist.

### Observability hooks
- `io.amichne.konditional.core.ops.RegistryHooks` is the single entry point for dependency-free logging/metrics adapters.

## Project-specific agent rules (from AGENTS.md)
- Prefer Kotlin expression bodies over control-flow heavy `return`.
- Prefer a single exit point (avoid multiple returns unless necessary).
- Avoid “Java-isms” when Kotlin has native alternatives.
- When presenting architectural choices / clarifying questions, use numbered options starting at 0.

## LLM-specific docs (repo-local)
`.llm-docs/` contains domain prompts and extracted context for this library (see `.llm-docs/README.md`). When writing docs or doing deep semantic analysis, prefer the relevant domain prompt and keep `core-types.kt` / `public-api-surface.md` up to date via `.llm-docs/scripts/extract-llm-context.sh`.
