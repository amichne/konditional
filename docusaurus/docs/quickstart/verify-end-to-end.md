# Verify end-to-end

Finish quickstart by proving your setup behaves as expected across declaration,
evaluation, rollout, and boundary handling.

## What you will achieve

You will run a compact verification checklist and leave a reusable validation
snippet in your codebase.

## Prerequisites

Complete [Load first snapshot safely](/quickstart/load-first-snapshot-safely).

## Main content

Run these checks:

1. **Typed evaluation check**
   - Confirm feature evaluation returns declared type at compile time.
2. **Determinism check**
   - Re-evaluate same context and confirm stable result.
3. **Rollout distribution check**
   - Sample many stable IDs and confirm approximate ramp-up percentage.
4. **Boundary failure check**
   - Feed invalid JSON and confirm `Result.failure` with parse details.
5. **No partial update check**
   - After a failed load, confirm prior behavior remains unchanged.

## Verification snippet

```kotlin
fun verifyQuickstart(jsonValid: String, jsonInvalid: String, userId: String) {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 0, 0),
        stableId = StableId.of(userId),
    )

    val first = AppFeatures.darkMode.evaluate(ctx)
    val repeated = (1..20).map { AppFeatures.darkMode.evaluate(ctx) }
    check(repeated.all { it == first })

    val ok = NamespaceSnapshotLoader(AppFeatures).load(jsonValid)
    check(ok.isSuccess)

    val bad = NamespaceSnapshotLoader(AppFeatures).load(jsonInvalid)
    check(bad.isFailure)
    check(bad.parseErrorOrNull() != null)
}
```

## Common issues

- treating quickstart checks as one-time work instead of regression checks;
- not preserving one known-invalid fixture for boundary testing;
- skipping alerting paths for parse failures.

## Next steps

- [How-to guides](/how-to-guides/rolling-out-gradually)
- [Runtime operations](/runtime/operations)
- [Troubleshooting](/troubleshooting/)
