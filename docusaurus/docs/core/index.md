# Core API

`konditional-core` is the minimal, stable API for defining and evaluating typed features in code.

If you only need compile-time correctness and deterministic evaluation, this is the only module you need.

## What you get

- Typed feature declarations for all features
- Evaluation of all features will always return a non-null value
- Rule DSL for targeting _(locale, platform, version, axes, custom predicates)_
- Deterministic ramp-ups _(stable bucketing)_
- Explainable evaluation and operational hooks _(`explain` for debugging)_

## Guarantees

- **Guarantee**: Feature access and return types are compile-time safe for statically-defined features.

- **Mechanism**: Generic type propagation on `Feature<T, C, M>`.

- **Boundary**: This does not apply to dynamically-generated feature definitions.

----

- **Guarantee**: Evaluation always returns a non-null value of the declared type.

- **Mechanism**: Every feature requires a `default` value and evaluation falls back to it.

- **Boundary**: Incorrect business logic still yields a value; Konditional does not validate intent.

## Quick example

```kotlin
import io.amichne.konditional.context.*

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { rampUp { 10.0 } }
    }
}

val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("user-123"),
)

val enabled = AppFeatures.darkMode.evaluate(ctx)
```

## Next steps

- [Rule DSL](/core/rules)
- [Core API reference](/core/reference)
- [Core types](/core/types)
- [Evaluation model](/fundamentals/evaluation-semantics)
