# Public API Surface Summary
# Extracted: 2026-01-25T15:12:32-05:00

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

- Learn the core concepts: [Core Concepts](/learn/core-primitives)
- Understand rule ordering and ramp-ups: [Evaluation Model](/learn/evaluation-model)

## From docusaurus/docs/api-reference/observability.md

---
title: Observability API (Legacy)
description: Legacy path for observability API reference.
unlisted: true
---

This page has moved.

See [Observability reference](/observability/reference/).

