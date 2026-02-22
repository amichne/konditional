# How-to: roll out a feature gradually

Use this page to ship a single feature in controlled percentage stages with
deterministic user assignment.

## Read this page when

- You are launching one feature behind a boolean flag.
- You need staged exposure with fast rollback.
- You need deterministic assignment for each user.

## Prerequisites

- A feature owner and rollout decision-maker.
- Stable `StableId` values for affected users.
- Guardrail metrics and alert thresholds.

## Deterministic steps

1. Define the flag with explicit salt and an initial threshold.

```kotlin
object CheckoutFlags : Namespace("checkout") {
    val newCheckout by boolean<Context>(default = false) {
        salt("checkout-rollout-v1")
        enable { rampUp { 10.0 } }
    }
}
```

2. Publish a rollout plan before changing percentages.

Use fixed stages, for example `10% -> 25% -> 50% -> 100%`, with explicit hold
criteria for each stage.

3. Evaluate the feature in production code.

```kotlin
val enabled = CheckoutFlags.newCheckout.evaluate(ctx)
```

4. Promote one stage at a time.

- Increase percentage only after metrics pass hold criteria.
- Keep salt constant during the same rollout campaign.
- Stop promotion on regressions.

5. Roll back deterministically when needed.

- Set rollout threshold to `0.0`, or
- Use namespace controls (`rollback(...)` or emergency `disableAll()`).

## Rollout checklist

- [ ] Stable ID source is durable and consistent.
- [ ] Rollout stages and hold criteria are documented.
- [ ] Salt value is fixed for the entire campaign.
- [ ] Guardrail metrics are monitored at each stage.
- [ ] Rollback path is tested before reaching broad exposure.

## Next steps

- [A/B testing](/how-to-guides/ab-testing)
- [Debugging determinism](/how-to-guides/debugging-determinism)
- [Refresh patterns](/production-operations/refresh-patterns)
