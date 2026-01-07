---
slug: /
---

# Konditional Core

Type-safe, deterministic feature flags for Kotlin.

Konditional Core is the smallest surface you need to define and evaluate features in code. You write features as typed
properties, pass a Context at runtime, and always get a value back.

## Start here

- [Installation](/getting-started/installation)
- [Your First Feature](/getting-started/your-first-flag)
- [Core Concepts](/fundamentals/core-primitives)
- [Evaluation Model](/fundamentals/evaluation-semantics)

## Quick example

```kotlin
import io.amichne.konditional.context.*
import io.amichne.konditional.core.dsl.enable

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        enable { ios() }
        enable { rampUp { 10.0 } }
    }
}

val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("user-123"),
)

val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
```

## Guarantees in core

### Type-safe access

**Guarantee**: Feature access and return types are compile-time safe for statically-defined features.

**Mechanism**: Property delegation and generic type propagation on `Feature<T, Context, Namespace>`.

**Boundary**: This does not apply to dynamically-generated features or external configuration systems.

### Total evaluation

**Guarantee**: Every evaluation returns a value of the declared type; no nulls and no missing cases.

**Mechanism**: Each feature requires a `default` value, which is returned when no rule matches.

**Boundary**: If your business logic is wrong, Konditional still returns a value; correctness is your responsibility.

### Deterministic ramp-ups

**Guarantee**: The same `(stableId, featureKey, salt)` always yields the same bucket assignment.

**Mechanism**: SHA-256 bucketing reduces the hash to a stable integer in `[0, 9999]`.

**Boundary**: Changing any of `stableId`, `featureKey`, or `salt` changes the bucket assignment.

## Why the guarantees hold

- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Type safety boundaries](/theory/type-safety-boundaries)

## What Konditional Core does not include

Konditional Core does not ship remote configuration, JSON serialization, or observability helpers. Those live in
separate modules:

- [Runtime](/runtime/index)
- [Serialization](/serialization/index)
- [Observability](/observability/index)
