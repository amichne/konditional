---
title: Golden path example
---

# Golden path example

This page gives a minimal end-to-end flow from typed definition to production
operations. It is intentionally compact and links to canonical procedures for
all details.

## Read this page when

- You want the shortest correct path to ship one feature safely.
- You need a reference sequence that combines implementation and operations.

## End-to-end slice

```kotlin
enum class CheckoutVariant { CONTROL, FAST_PATH }

object CheckoutFlags : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CONTROL) {
        rule(CheckoutVariant.FAST_PATH) { rampUp { 10.0 } }
    }
}

fun decide(userId: String): CheckoutVariant {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of(userId),
    )
    return CheckoutFlags.variant.evaluate(ctx)
}
```

## Deterministic steps

1. Implement the typed feature definition and local evaluation call.
2. Apply [Roll out gradually](/how-to-guides/rolling-out-gradually).
3. Add experiment measurement via [A/B testing](/how-to-guides/ab-testing)
   when variants matter.
4. Load configuration updates through
   [Safe remote config](/how-to-guides/safe-remote-config).
5. Operate refresh and incident handling with
   [Refresh patterns](/production-operations/refresh-patterns) and
   [Failure modes](/production-operations/failure-modes).

## Verification checklist

- [ ] Same `(stableId, featureKey, salt)` always returns the same outcome.
- [ ] Invalid snapshots are rejected without partial activation.
- [ ] Rollback and kill-switch paths are tested before production rollout.

## Next steps

- [Testing features](/how-to-guides/testing-features)
- [Operational debugging](/production-operations/debugging)
- [Troubleshooting index](/troubleshooting/)
