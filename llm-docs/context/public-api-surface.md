# Public API Surface Summary
# Extracted: 2026-01-14T08:33:50-05:00

## From docusaurus/docs/index.md

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

- **Guarantee**: Feature access and return types are compile-time safe for statically-defined features.

- **Mechanism**: Property delegation and generic type propagation on `Feature<T, Context, Namespace>`.

- **Boundary**: This does not apply to dynamically-generated features or external configuration systems.

### Total evaluation

- **Guarantee**: Every evaluation returns a value of the declared type; no nulls and no missing cases.

- **Mechanism**: Each feature requires a `default` value, which is returned when no rule matches.

- **Boundary**: If your business logic is wrong, Konditional still returns a value; correctness is your responsibility.

### Deterministic ramp-ups

- **Guarantee**: The same `(stableId, featureKey, salt)` always yields the same bucket assignment.

- **Mechanism**: SHA-256 bucketing reduces the hash to a stable integer in `[0, 9999]`.

- **Boundary**: Changing any of `stableId`, `featureKey`, or `salt` changes the bucket assignment.

## Why the guarantees hold

- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Type safety boundaries](/theory/type-safety-boundaries)

## What Konditional Core does not include

Konditional Core does not ship remote configuration, JSON serialization, or observability helpers. Those live in
separate modules:

- [Runtime](/runtime/)
- [Serialization](/serialization/)
- [Observability](/observability/)

## From docusaurus/docs/getting-started/installation.md

# Installation

Konditional Core is a single dependency.

Replace `VERSION` with the latest published version.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  implementation("io.amichne:konditional-core:VERSION")
}
```

## Test Fixtures (Optional)

Konditional provides test helpers for common testing scenarios. Add the `testFixtures` dependency to your test
configuration:

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  testImplementation(testFixtures("io.amichne:konditional-core:VERSION"))
}
```

**Available test helpers:**

- `CommonTestFeatures` — Pre-configured feature flags for common testing scenarios
- `EnterpriseTestFeatures` — Enterprise-tier feature flags for advanced testing
- `TestAxis` — Axis definitions for testing multi-dimensional targeting
- `TestNamespace` — Namespace implementations for testing
- `TestStableId` — StableId utilities for deterministic test buckets
- `TargetingIds` — Pre-computed IDs for specific bucket targeting
- `FeatureMutators` — Utilities for modifying feature configurations in tests

See [How-To: Test Your Feature Flags](/how-to-guides/testing-features) for usage examples.

---

That is enough to define features and evaluate them in code. If you need remote configuration, JSON serialization, or
observability utilities, see the module docs:

- [Runtime](/runtime/)
- [Serialization](/serialization/)
- [Observability](/observability/)

## From docusaurus/docs/getting-started/your-first-flag.md

# Your First Feature

This guide builds one feature end-to-end: definition, targeting rules, and evaluation.

## 1) Define a namespace and a feature

```kotlin
import io.amichne.konditional.context.*

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { locales(AppLocale.UNITED_STATES) }
    }
}
```

## 2) Create a context and evaluate

```kotlin
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("user-123"),
)

val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
```

If no rule matches, the default value is returned.

## What just happened

- A **Namespace** is a registry of features.
- A **Feature** is a typed value with rules.
- A **Rule** is criteria -> value mapping.
- A **Context** provides runtime inputs used by rules.

## Guarantees

- **Guarantee**: Evaluation always returns a non-null value of the declared type.

- **Mechanism**: Features require a `default` value and return it when no rule matches.

- **Boundary**: Konditional does not validate business logic; it only evaluates rules.

## Next steps

- Learn the core concepts: [Core Concepts](/fundamentals/core-primitives)
- Understand rule ordering and ramp-ups: [Evaluation Model](/fundamentals/evaluation-semantics)

## From docusaurus/docs/fundamentals/core-primitives.md

# Core Concepts

This page defines the minimum vocabulary you need to read and write Konditional features.

## Terms

- **Namespace**: A registry that owns a set of features.
- **Feature**: A typed configuration value with rules and a default.
- **Context**: Runtime inputs used for evaluation (`locale`, `platform`, `appVersion`, `stableId`).
- **Rule**: Criteria -> value mapping. All criteria must match for the rule to apply.
- **Specificity**: A numeric measure of how constrained a rule is. Higher specificity wins.
- **Bucketing**: Deterministic assignment of a `stableId` to a ramp-up bucket.

## Compile-time vs runtime

| Aspect                     | Guarantee Level | Mechanism                                                  |
|----------------------------|-----------------|------------------------------------------------------------|
| Property access            | Compile-time    | Property delegation on `Namespace`                         |
| Return types               | Compile-time    | Generic type propagation (`Feature<T, C, M>`)              |
| Rule values                | Compile-time    | Typed DSL builders (`boolean`, `string`, `enum`, `custom`) |
| Non-null returns           | Compile-time    | Required defaults                                          |
| Rule matching              | Runtime         | Deterministic evaluation over `Context`                    |
| Business logic correctness | Not guaranteed  | Human responsibility                                       |

## Typed values in practice

```kotlin
enum class Theme { LIGHT, DARK }

object AppFeatures : Namespace("app") {
  val darkMode by boolean<Context>(default = false)
  val theme by enum<Theme, Context>(default = Theme.LIGHT)
  val retries by integer<Context>(default = 3)
}

val theme: Theme = AppFeatures.theme.evaluate(ctx)
```

## Type-safety guarantee

- **Guarantee**: Feature access and return types are compile-time safe for statically-defined features.

- **Mechanism**: Feature properties are declared with explicit type parameters and enforced by the Kotlin type system.

- **Boundary**: Dynamically-generated features are outside this guarantee.

## From docusaurus/docs/fundamentals/evaluation-semantics.md

# Evaluation Model

This page explains how Konditional chooses a value when multiple rules exist.

## Evaluation order

When you call `feature.evaluate(context)`:

1. Rules are sorted by **specificity** (highest first).
2. Each rule is checked against the Context.
3. If a rule matches, ramp-up is applied (if configured).
4. The first matching rule that passes ramp-up wins.
5. If nothing matches, the default value is returned.

## Specificity system

Specificity is the sum of targeting constraints and custom predicate specificity.

**Base targeting specificity** (0-3+):

- `locales(...)` adds 1 if non-empty
- `platforms(...)` adds 1 if non-empty
- `versions { ... }` adds 1 if bounded
- `axis(...)` adds 1 per axis constraint

**Custom predicate specificity**:

- A custom `Predicate` can define its own `specificity()`
- Default predicate specificity is 1

- **Guarantee**: More specific rules are evaluated before less specific rules.

- **Mechanism**: Rules are sorted by `rule.specificity()` in descending order before evaluation.

- **Boundary**: Ramp-up percentage does not affect specificity.

## Deterministic ramp-ups

Ramp-ups are deterministic and reproducible.

- **Guarantee**: The same `(stableId, featureKey, salt)` always yields the same bucket assignment.

- **Mechanism**:

1. Hash the UTF-8 bytes of `"$salt:$featureKey:${stableId.hexId.id}"` with SHA-256.
2. Convert the first 4 bytes to an unsigned 32-bit integer.
3. Bucket = `hash % 10_000` (range `[0, 9999]`).
4. Threshold = `(rampUp.value * 100.0).roundToInt()` (basis points).
5. In ramp-up if `bucket < threshold`.

- **Boundary**: Changing `stableId`, `featureKey`, or `salt` changes the bucket assignment.

## Example

```kotlin
object AppFeatures : Namespace("app") {
    val checkout by string<Context>(default = "v1") {
        rule("v3") { platforms(Platform.IOS); versions { min(3, 0, 0) } } // specificity 2
        rule("v2") { platforms(Platform.IOS) }                            // specificity 1
        rule("v1") { always() }                                           // specificity 0
    }
}
```

For an iOS user on version 3.1.0, the `v3` rule is evaluated first and wins if it matches.

