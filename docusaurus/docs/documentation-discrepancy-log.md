# Documentation Discrepancy Log

This page tracks places where the documentation previously diverged from the Kotlin code (source of truth), and where
the docs were updated to match the real public surface + semantics.

If you introduce a new public API or change semantics, add an entry here and update the referenced doc pages.

---

## D-001 — Published coordinates (`groupId`)

- **Docs previously implied:** `io.github.amichne:konditional:...`
- **Code reality:** group is `io.amichne` (see `gradle.properties`).
- **Docs updated:** `/` (`docusaurus/docs/index.md`), `/getting-started/installation` (`docusaurus/docs/getting-started/installation.md`)

---

## D-002 — Runtime baseline (Kotlin/JVM)

- **Docs previously implied:** Kotlin 1.9+ / JVM 11+, multiplatform.
- **Code reality:** Kotlin plugin is `2.2.20` and the library targets Java 21 bytecode (`jvmToolchain(21)`,
  `targetCompatibility = "21"`).
- **Docs updated:** `/getting-started/installation` (`docusaurus/docs/getting-started/installation.md`)

---

## D-003 — Flag delegate context type parameter is required

- **Docs previously showed:** `val flag by boolean(default = false)` (and similar for `string/integer/double/enum/custom`).
- **Code reality:** delegate factories are generic on `C : Context` (e.g. `boolean<C : Context>(...)`), so examples must
  specify the context type (commonly `boolean<Context>(...)`) unless the property type constrains it.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt`
- **Docs updated:** `/` (`docusaurus/docs/index.md`), `/why-konditional` (`docusaurus/docs/why-konditional.md`),
  `/api-reference/core-types` (`docusaurus/docs/api-reference/core-types.md`),
  `/theory/namespace-isolation` (`docusaurus/docs/theory/namespace-isolation.md`)

---

## D-004 — `FeatureId` (stable serialized identifier) vs `FeatureKey`

- **Docs previously used:** `FeatureKey` as the stable identifier.
- **Code reality:** stable serialized identifiers are `FeatureId` (format `feature::<namespaceSeed>::<key>`, with legacy
  `value::...` support); `Feature.key` is the logical key (property name).
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/values/FeatureId.kt`,
  `core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt`
- **Docs updated:** `/api-reference/core-types` (`docusaurus/docs/api-reference/core-types.md`),
  `/persistence-format` (`docusaurus/docs/persistence-format.md`),
  `/theory/namespace-isolation` (`docusaurus/docs/theory/namespace-isolation.md`)

---

## D-005 — `Namespace` is the registry (no public `.registry`)

- **Docs previously used:** `namespace.registry` and/or a global registry factory.
- **Code reality:** `Namespace` implements `NamespaceRegistry` via delegation; the backing registry is internal and there
  is no supported public `.registry` access pattern.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt`,
  `core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt`
- **Docs updated:** `/api-reference/core-types` (`docusaurus/docs/api-reference/core-types.md`),
  `/api-reference/namespace-operations` (`docusaurus/docs/api-reference/namespace-operations.md`),
  `/theory/namespace-isolation` (`docusaurus/docs/theory/namespace-isolation.md`),
  `/fundamentals/refresh-safety` (`docusaurus/docs/fundamentals/refresh-safety.md`)

---

## D-006 — `Configuration` shape and keying

- **Docs previously implied:** `Configuration.flags` keyed by string-ish ids and nullable metadata.
- **Code reality:** `Configuration` has an `internal constructor`, `metadata` is non-null, and `flags` is keyed by
  `Feature<*,*,*>` instances (structural guarantee).
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/instance/Configuration.kt`
- **Docs updated:** `/api-reference/core-types` (`docusaurus/docs/api-reference/core-types.md`),
  `/theory/determinism-proofs` (`docusaurus/docs/theory/determinism-proofs.md`)

---

## D-007 — ParseResult access patterns (`.value` vs helpers)

- **Docs previously used:** `SnapshotSerializer.fromJson(...).value` / `ParseResult.value` as if it were always present.
- **Code reality:** `ParseResult` is sealed; access the value via `when` on `ParseResult.Success`, or use
  `ParseResult.getOrThrow()` when you want fail-fast behavior.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt`
- **Docs updated:** `/api-reference/*` and theory pages that previously used `.value` directly

---

## D-008 — Parse error taxonomy (no `ParseError.TypeMismatch`)

- **Docs previously referenced:** `ParseError.TypeMismatch`.
- **Code reality:** decode/shape/type issues are represented as `ParseError.InvalidSnapshot` (and malformed JSON as
  `ParseError.InvalidJson`).
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt`
- **Docs updated:** `/advanced/testing-strategies` (`docusaurus/docs/advanced/testing-strategies.md`),
  `/theory/type-safety-boundaries` (`docusaurus/docs/theory/type-safety-boundaries.md`),
  `/fundamentals/failure-modes` (`docusaurus/docs/fundamentals/failure-modes.md`)

---

## D-009 — JSON parsing implementation (Moshi, not kotlinx.serialization)

- **Docs previously implied:** `kotlinx.serialization`.
- **Code reality:** snapshots/patches are parsed with Moshi (plus custom adapters).
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt`
- **Docs updated:** `/theory/type-safety-boundaries` (`docusaurus/docs/theory/type-safety-boundaries.md`)

---

## D-010 — Serialization model types (`Serializable*`) should not be referenced in public docs

- **Docs previously named:** `SerializableSnapshot`, `SerializableFlag`, `SerializableRule`, etc., in diagrams/templates.
- **Code reality:** those types live under `io.amichne.konditional.internal.serialization.models` and are implementation
  details of the JSON boundary; the stable boundary is the JSON shape itself.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/*`
- **Docs updated:** `/api-reference/serialization` (`docusaurus/docs/api-reference/serialization.md`),
  `/persistence-format` (`docusaurus/docs/persistence-format.md`)

---

## D-011 — Observability hooks are per-registry, not global callbacks

- **Docs previously showed:** global `RegistryHooks.onEvaluationComplete` / `onParseFailure` style hooks.
- **Code reality:** `RegistryHooks` is a value object (`logger`, `metrics`) installed per registry via
  `NamespaceRegistry.setHooks(...)`; parse failures are surfaced at the parse boundary (`ParseResult.Failure`), not via a
  hook.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt`,
  `core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt`
- **Docs updated:** `/api-reference/observability` (`docusaurus/docs/api-reference/observability.md`),
  `/advanced/multiple-namespaces` (`docusaurus/docs/advanced/multiple-namespaces.md`),
  `/advanced/testing-strategies` (`docusaurus/docs/advanced/testing-strategies.md`)

---

## D-012 — Metrics payload types and fields

- **Docs previously used:** `EvaluationEvent`-style payloads.
- **Code reality:** `MetricsCollector` receives `Metrics.Evaluation`, `Metrics.ConfigLoadMetric`, `Metrics.ConfigRollbackMetric`.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt`,
  `core/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt`
- **Docs updated:** `/api-reference/observability` (`docusaurus/docs/api-reference/observability.md`),
  `/advanced/testing-strategies` (`docusaurus/docs/advanced/testing-strategies.md`),
  `/advanced/multiple-namespaces` (`docusaurus/docs/advanced/multiple-namespaces.md`)

---

## D-013 — Shadow evaluation API + mismatch structure

- **Docs previously used:** `ShadowEvaluationOptions`, mismatch values returned from `evaluateShadow`, and/or mismatch
  objects containing `context`.
- **Code reality:** `ShadowOptions` configures shadowing; `evaluateWithShadow(...)` returns the baseline value and emits a
  `ShadowMismatch<T>` (baseline/candidate are `EvaluationResult<T>`); `evaluateShadow(...)` returns `Unit` and is
  callback-driven.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt`
- **Docs updated:** `/api-reference/feature-operations` (`docusaurus/docs/api-reference/feature-operations.md`),
  `/advanced/shadow-evaluation` (`docusaurus/docs/advanced/shadow-evaluation.md`),
  `/theory/migration-and-shadowing` (`docusaurus/docs/theory/migration-and-shadowing.md`),
  `/api-reference/observability` (`docusaurus/docs/api-reference/observability.md`)

---

## D-014 — Snapshot parsing requires namespace initialization (feature registration)

- **Docs previously contained examples that:** called `SnapshotSerializer.fromJson(...)` before ensuring namespaces were
  initialized.
- **Code reality:** deserialization resolves each `FeatureId` via an internal registry populated during namespace
  initialization (delegated properties). Without registration, parsing fails with `ParseError.FeatureNotFound`.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt`,
  `core/src/main/kotlin/io/amichne/konditional/serialization/ConversionUtils.kt`
- **Docs updated:** `/api-reference/feature-operations` (`docusaurus/docs/api-reference/feature-operations.md`),
  `/api-reference/namespace-operations` (`docusaurus/docs/api-reference/namespace-operations.md`),
  `/advanced/shadow-evaluation` (`docusaurus/docs/advanced/shadow-evaluation.md`),
  `/advanced/testing-strategies` (`docusaurus/docs/advanced/testing-strategies.md`),
  `/fundamentals/refresh-safety` (`docusaurus/docs/fundamentals/refresh-safety.md`),
  `/fundamentals/trust-boundaries` (`docusaurus/docs/fundamentals/trust-boundaries.md`),
  `/theory/parse-dont-validate` (`docusaurus/docs/theory/parse-dont-validate.md`),
  `/theory/namespace-isolation` (`docusaurus/docs/theory/namespace-isolation.md`),
  `/theory/migration-and-shadowing` (`docusaurus/docs/theory/migration-and-shadowing.md`)

---

## D-015 — Atomicity pseudo-code drift (`getConfig()` / internal mutation)

- **Docs previously referenced:** `getConfig()`-style accessors and a hypothetical mutable internal registry state.
- **Code reality:** public reads are via `NamespaceRegistry.configuration`; internal state mutation is not a supported
  public operation.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt`,
  `core/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt`
- **Docs updated:** `/theory/atomicity-guarantees` (`docusaurus/docs/theory/atomicity-guarantees.md`),
  `/fundamentals/refresh-safety` (`docusaurus/docs/fundamentals/refresh-safety.md`)

---

## D-016 — Naming collision in docs: “FeatureRegistry” (DI example)

- **Docs previously showed:** a DI bootstrap class named `FeatureRegistry`, which can be confused with Konditional’s
  internal feature registry used for deserialization.
- **Docs updated:** `/fundamentals/definition-vs-initialization` (`docusaurus/docs/fundamentals/definition-vs-initialization.md`)

---

## D-017 — Prefer `Feature(context)` / `explain(...)` over `evaluate(...)`

- **Docs previously used:** `feature.evaluate(context)` as the primary evaluation API at call sites.
- **Code reality:** `Feature.evaluate(...)` is now annotated with `@VerboseApi` (a Kotlin `@RequiresOptIn(level = ERROR)` marker).
  The preferred call-site API is the operator overload `Feature(context)`, and the preferred debugging API is `feature.explain(context)`.
- **Code references:** `core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt`
- **Docs updated:** `/api-reference/feature-operations` (`docusaurus/docs/api-reference/feature-operations.md`),
  `/` (`docusaurus/docs/index.md`),
  `/why-konditional` (`docusaurus/docs/why-konditional.md`),
  `/migration` (`docusaurus/docs/migration.md`),
  `/getting-started/your-first-flag` (`docusaurus/docs/getting-started/your-first-flag.md`),
  `/getting-started/loading-from-json` (`docusaurus/docs/getting-started/loading-from-json.md`),
  `/fundamentals/core-primitives` (`docusaurus/docs/fundamentals/core-primitives.md`),
  `/fundamentals/configuration-lifecycle` (`docusaurus/docs/fundamentals/configuration-lifecycle.md`),
  `/fundamentals/evaluation-semantics` (`docusaurus/docs/fundamentals/evaluation-semantics.md`),
  `/fundamentals/failure-modes` (`docusaurus/docs/fundamentals/failure-modes.md`),
  `/fundamentals/refresh-safety` (`docusaurus/docs/fundamentals/refresh-safety.md`),
  `/fundamentals/trust-boundaries` (`docusaurus/docs/fundamentals/trust-boundaries.md`),
  `/advanced/custom-context-types` (`docusaurus/docs/advanced/custom-context-types.md`),
  `/advanced/multiple-namespaces` (`docusaurus/docs/advanced/multiple-namespaces.md`),
  `/advanced/shadow-evaluation` (`docusaurus/docs/advanced/shadow-evaluation.md`),
  `/advanced/kontracts-deep-dive` (`docusaurus/docs/advanced/kontracts-deep-dive.md`),
  `/advanced/testing-strategies` (`docusaurus/docs/advanced/testing-strategies.md`),
  `/rules-and-targeting/custom-extensions` (`docusaurus/docs/rules-and-targeting/custom-extensions.md`),
  `/rules-and-targeting/rollout-strategies` (`docusaurus/docs/rules-and-targeting/rollout-strategies.md`),
  `/theory/atomicity-guarantees` (`docusaurus/docs/theory/atomicity-guarantees.md`),
  `/theory/determinism-proofs` (`docusaurus/docs/theory/determinism-proofs.md`),
  `/theory/migration-and-shadowing` (`docusaurus/docs/theory/migration-and-shadowing.md`),
  `/theory/type-safety-boundaries` (`docusaurus/docs/theory/type-safety-boundaries.md`)
