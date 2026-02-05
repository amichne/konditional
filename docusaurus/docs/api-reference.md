---
title: API Reference
---

# API Reference (Single Page)

This page is a “single node” API map of Konditional’s consumer-facing surface, with the most important compile-time vs
runtime boundaries made explicit.

:::note Scope
Konditional’s API surface spans multiple modules. `:konditional-core` defines the types and evaluation semantics;
runtime lifecycle and JSON boundaries live in sibling modules.
:::

---

## Which module provides what?

| Module | What it owns | Why it exists |
|---|---|---|
| `konditional-core` | DSL + evaluation engine + core types | Compile-time correctness + deterministic evaluation |
| `konditional-runtime` | Lifecycle operations (load/rollback/history) | Explicit operational surface; not in core |
| `konditional-serialization` | JSON codecs and patch application | Validated trust boundary (`ParseResult`/`ParseError`) |
| `konditional-observability` | Shadow evaluation + operational patterns | Safe migrations, mismatch tracing |

---

## Feature evaluation (`evaluate` / `explain`)

**Primary entrypoints:**

- `Feature.evaluate(context, registry = namespace): T`
- `Feature.explain(context, registry = namespace): EvaluationResult<T>`

**Source:** `konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt`

:::tip Pick the cheapest tool that works
Use `evaluate` on hot paths; use `explain` for debugging, tooling, and diagnosis.
:::

Minimal:

```kotlin
val enabled: Boolean = AppFlags.darkMode.evaluate(context)
val result = AppFlags.darkMode.explain(context)
```

<details>
<summary>What can an explanation decide?</summary>

`EvaluationResult.Decision` includes:

- `RegistryDisabled` (namespace kill-switch)
- `Inactive` (flag definition inactive)
- `Rule` (matched a rule; includes bucket info)
- `Default` (no rule matched; may include “skipped by rollout” info)

See `konditional-core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt`.

</details>

<details>
<summary>Compatibility: evaluateWithReason (deprecated)</summary>

`Feature.evaluateWithReason(...)` is deprecated in favor of `explain(...)` for clearer intent.

See `konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt`.

</details>

---

## Defining flags (Namespace property delegates)

Flags are defined as delegated properties on a `Namespace`. The delegate determines:

- value type `T` (boolean/int/double/string/enum/custom)
- context type `C` the feature evaluates against
- namespace binding `M` (compile-time isolation)

**Source:** `konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt`

Minimal:

```kotlin
object Payments : Namespace("payments") {
    val applePayEnabled by boolean<Context>(default = false)
}
```

:::note Compile-time guarantee
At call sites, `Payments.applePayEnabled.evaluate(context)` returns `Boolean` without casts or runtime coercion.
:::

---

## Namespace registry surface (core)

Namespaces are also registries (by delegation): lifecycle operations and hooks are reachable on the namespace object.

**Source:** `konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt`

Key members:

- `val configuration: ConfigurationView`
- `val isAllDisabled: Boolean`
- `fun disableAll()`, `fun enableAll()`
- `val hooks: RegistryHooks`, `fun setHooks(hooks: RegistryHooks)`
- `fun flag(feature): FlagDefinition<...>` (read the active definition)

:::danger Kill-switch semantics
The kill-switch forces all evaluations in that namespace to return declared defaults. It does not mutate definitions or
remote config state.
:::

---

## Rule DSL surface (`FlagScope` / `RuleScope`)

Rule authoring is scoped to two DSL receivers:

- `FlagScope<T, C, M>` — add rules, set salt, allowlists, activation, composition
- `RuleScope<C>` — express targeting constraints and yield values

**Sources:**

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt`
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt`

Criteria-first rule:

```kotlin
val checkout by string<Context>(default = "v1") {
    rule {
        platforms(Platform.IOS)
        versions { min(3, 0, 0) }
        rampUp { 25.0 }
        note("iOS v2 rollout")
    } yields "v2"
}
```

:::tip Reference
See [Rule Model](/rules) and [DSL Surface](/dsl-authoring) for the full authoring story (with precedence rules).
:::

---

## Runtime operations (`load` / `rollback` / `history`)

Lifecycle operations are intentionally not part of the `:konditional-core` surface.

**Source:** `konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt`

- `fun Namespace.load(configuration: ConfigurationView)`
- `fun Namespace.rollback(steps: Int = 1): Boolean`
- `val Namespace.history: List<ConfigurationView>`
- `val Namespace.historyMetadata: List<ConfigurationMetadataView>`

Typical boundary usage:

```kotlin
val json = fetchRemoteConfig()
when (val decoded = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFlags.load(decoded.value)
    is ParseResult.Failure -> logger.error("Config rejected: ${decoded.error.message}")
}
```

---

## Context capabilities and axes

Targeting operators are only expressible when the feature’s context type supports them.

**Core context capability type:** `konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt`

Axes:

- `Axis<T>` (definition) and `AxisValue<T>` (stable value IDs)
- `Context.axisValues` to provide axis values at runtime

**Sources:**

- `konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt`
- `konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt`

---

## JSON boundary (`ConfigurationSnapshotCodec`)

The JSON boundary is explicit: decoding returns a `ParseResult`, not a partially-valid value.

**Source:** `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt`

- `fun encode(value: ConfigurationView): String`
- `fun decode(json: String, options = SnapshotLoadOptions.strict()): ParseResult<Configuration>`
- `fun applyPatchJson(currentConfiguration: ConfigurationView, patchJson: String, options = ...): ParseResult<Configuration>`

:::note Pure vs side-effecting
`ConfigurationSnapshotCodec` is pure: it does not mutate any namespace. To parse+load in one step, use
`NamespaceSnapshotLoader`.
:::

---

## ParseResult / ParseError

`ParseResult<T>` forces callers to handle failures before configuration becomes active.

**Sources:**

- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt`
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt`
- `konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt`

Common helpers:

```kotlin
val config = ConfigurationSnapshotCodec.decode(json)
    .onFailure { error -> logger.error(error.message) }
    .getOrNull()
```

---

## Side-effecting snapshot loader (`NamespaceSnapshotLoader`)

`NamespaceSnapshotLoader` decodes JSON (via a codec) and loads successful results into the namespace’s runtime registry.

**Source:** `konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt`

```kotlin
val loader = NamespaceSnapshotLoader(AppFlags)
val result = loader.load(json)
```

:::caution Dependency note
`NamespaceSnapshotLoader` requires `:konditional-runtime` (it needs `NamespaceRegistryRuntime`).
:::

---

## Deterministic bucketing (`RampUpBucketing`)

Use `RampUpBucketing` for production debugging (“why is user X in/out of 10%?”).

**Source:** `konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt`

```kotlin
val info = RampUpBucketing.explain(
    stableId = StableId.of("user-123"),
    featureKey = "feature::checkout::newUi",
    salt = "v1",
    rampUp = RampUp.of(10.0),
)
```

---

## Shadow evaluation (observability)

Shadow evaluation compares baseline vs candidate registries without changing production behavior.

**Source:** `konditional-observability` (see Recipes and the module docs samples)

```kotlin
val value = AppFlags.darkMode.evaluateWithShadow(
    context = context,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        logger.warn("shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
    },
)
```

:::warning Hot-path cost
Shadow evaluation adds a second evaluation. Sample requests if needed.
:::

---

## Glossary

If a term here is unfamiliar (Namespace vs registry, snapshot vs configuration, etc.), use:

- [Glossary](/glossary)
