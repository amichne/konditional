# Evaluate in app code

Evaluate your feature using a typed context and confirm deterministic behavior
for fixed inputs.

## What you will achieve

You will wire feature evaluation into application code and validate stable
results for the same context.

## Prerequisites

Complete [Define first flag](/quickstart/define-first-flag).

## Main content

Create one context and evaluate your feature:

```kotlin
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("user-123"),
)

val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
```

Use the result directly in app flow:

```kotlin
if (AppFeatures.darkMode.evaluate(ctx)) {
    enableDarkMode()
} else {
    useDefaultTheme()
}
```

## Verify

Validate deterministic behavior with repeated evaluation:

```kotlin
val repeated = (1..20).map { AppFeatures.darkMode.evaluate(ctx) }
check(repeated.all { it == repeated.first() })
```

## Common issues

- Building `stableId` from session-local values.
- Assuming different snapshots should return identical results.
- Using different contexts across repeated checks.

## Next steps

- [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up)
- [Determinism and bucketing](/theory/determinism-proofs)
