# How-to: debug determinism issues

Use this page to diagnose non-deterministic rollout behavior with a fixed,
repeatable workflow.

## Read this page when

- The same user appears to switch variants unexpectedly.
- Ramp-up percentages drift from expected distribution.
- Determinism regressions appear after config or code changes.

## Deterministic steps

1. Capture one deterministic tuple to debug.

Record `stableId`, `featureKey`, `salt`, ramp percentage, and timestamp from a
real request before changing anything.

2. Verify `stableId` construction is stable across all code paths.

- Confirm the same durable user identifier is used everywhere.
- Reject request/session IDs as rollout identifiers.
- Log both source ID and derived `StableId` for the sampled requests.

3. Explain bucket assignment directly.

```kotlin
val explanation = RampUpBucketing.explain(
    stableId = StableId.of("user-123"),
    featureKey = AppFeatures.newCheckout.key,
    salt = "checkout-v1",
    rampUp = RampUp.of(25.0),
)
println("bucket=${explanation.bucket} inRollout=${explanation.inRollout}")
```

4. Validate rule order and cumulative ramp ranges.

- Earlier matching rules win.
- Use cumulative thresholds for variant splits.
- Treat `salt(...)` changes as intentional re-sampling events.

5. Replay the same tuple against local and production configs.

```kotlin
val first = AppFeatures.newCheckout.evaluate(ctx)
val second = AppFeatures.newCheckout.evaluate(ctx)
check(first == second)
```

6. Lock in a regression test before closing the issue.

Add a test fixture that uses the captured tuple and asserts the expected result.

## Verification checklist

- [ ] Stable ID source is durable and consistent.
- [ ] Bucket explanation matches expected threshold behavior.
- [ ] Rule ordering and salt usage are documented and intentional.
- [ ] Replay test passes repeatedly with identical output.
- [ ] A determinism regression test was added.

## Next steps

- [A/B testing](/how-to-guides/ab-testing)
- [Troubleshooting](/troubleshooting/)
- [Operational debugging](/production-operations/debugging)
