# Define first flag

Define one typed feature in a namespace so your key and value type are checked
at compile time.

## What you will achieve

You will create one namespace and one feature with a default and simple
targeting rules.

## Prerequisites

Complete [Install](/quickstart/install) first.

## Main content

Create a namespace and define a feature:

```kotlin
import io.amichne.konditional.context.*
import io.amichne.konditional.core.Namespace

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { locales(AppLocale.UNITED_STATES) }
    }
}
```

This establishes:

- a compile-time feature key (`darkMode`);
- a fixed return type (`Boolean`);
- a non-null default value (`false`).

## Verify

Compile and ensure the feature is available as a typed property.

```kotlin
val enabled: Boolean = AppFeatures.darkMode.evaluate(
    Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(1, 0, 0),
        stableId = StableId.of("user-123"),
    ),
)
```

## Common issues

- Missing default value in a feature declaration.
- Wrong context type for a strongly-typed feature declaration.
- Importing context or namespace types from the wrong package.

## Next steps

- [Evaluate in app code](/quickstart/evaluate-in-app-code)
- [Why typed flags](/overview/why-typed-flags)
