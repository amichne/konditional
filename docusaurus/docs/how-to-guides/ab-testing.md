# How-to: run A/B variant experiments

Use this page to run deterministic multi-variant experiments with typed results,
stable assignment, and explicit rollout decisions.

## Read this page when

- You need two or more treatment variants for the same feature.
- You must keep assignment stable for each user across requests.
- You need clear winner criteria before promotion.

## Prerequisites

- A durable user identifier that maps to `StableId`.
- A namespace that owns the experiment flag.
- Analytics events for assignment and outcome.

## Deterministic steps

1. Model experiment outcomes as a closed enum.

```kotlin
enum class CheckoutVariant {
    CONTROL,
    SIMPLIFIED,
    ENHANCED,
}
```

2. Define cumulative allocation rules with a fixed salt.

```kotlin
object CheckoutExperiments : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CONTROL) {
        salt("checkout-exp-v1")
        rule(CheckoutVariant.SIMPLIFIED) { rampUp { 33.0 } }
        rule(CheckoutVariant.ENHANCED) { rampUp { 66.0 } }
    }
}
```

3. Evaluate once per request and branch exhaustively.

```kotlin
val variant = CheckoutExperiments.variant.evaluate(ctx)
when (variant) {
    CheckoutVariant.CONTROL -> renderControl()
    CheckoutVariant.SIMPLIFIED -> renderSimplified()
    CheckoutVariant.ENHANCED -> renderEnhanced()
}
```

4. Emit assignment and outcome events with the same variant label.

- Assignment event: when you decide the variant.
- Outcome event: when the success metric occurs.
- Required keys: `featureKey`, `variant`, `stableId`, and timestamp.

5. Promote or stop the experiment using explicit criteria.

- Promote winner: set default to winner and remove experiment rules.
- Stop experiment: keep default control and remove experiment rules.
- Re-sample intentionally: change `salt(...)` only when starting a new test.

## Verification checklist

- [ ] Variant type is closed and evaluated with exhaustive `when`.
- [ ] Ramp rules are cumulative and ordered intentionally.
- [ ] Same user receives the same variant across repeated evaluations.
- [ ] Assignment and outcome telemetry use identical variant keys.
- [ ] Promotion and rollback criteria are documented before launch.

## Next steps

- [Roll out gradually](/how-to-guides/rolling-out-gradually)
- [Debugging determinism](/how-to-guides/debugging-determinism)
- [Operational debugging](/production-operations/debugging)
