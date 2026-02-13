# Your First Feature

This guide helps you ship one safe feature toggle end-to-end: definition, targeting rules, and evaluation.

---

## Prerequisites

- Konditional installed: [Installation](/getting-started/installation)
- A Kotlin project where you can run a small local check

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

---

## Verification

Evaluate the same context multiple times and confirm the returned value is stable for that input.

```kotlin
val repeated = (1..20).map { AppFeatures.darkMode.evaluate(ctx) }
check(repeated.all { it == repeated.first() })
```

---

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

- Roll out safely: [How-To: Roll Out a Feature Gradually](/how-to-guides/rolling-out-gradually)
- Learn the core concepts: [Core Concepts](/learn/core-primitives)
- Understand rule ordering and ramp-ups: [Evaluation Model](/learn/evaluation-model)
