---
title: Add Deterministic Ramp-Up
sidebar_position: 5
---

# Add Deterministic Ramp-Up

Add one rollout rule that is stable for the same identity input.

**Prerequisites:** You have completed [Evaluate in App Code](/quickstart/evaluate-in-app-code).

<span id="claim-clm-pr01-10a"></span>
Ramp-up assignment is deterministic and exposed through stable bucket APIs.

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId

object RolloutFeatures : Namespace("rollout") {
  val newCheckout by boolean<Context>(default = false) {
    rule(true) {
      rampUp { 25 }
    }
  }
}

val sameUser = Context(AppLocale.UNITED_STATES, Platform.IOS, Version.of(2, 1, 0), StableId.of("user-123"))
val first = RolloutFeatures.newCheckout.evaluate(sameUser)
val second = RolloutFeatures.newCheckout.evaluate(sameUser)
check(first == second)
```

## Verify

Use `RampUpBucketing.bucket(stableId, featureKey, salt)` when you need explicit bucket introspection for debugging and audit traces.

## Expected Outcome

After this step, the same stable identity is consistently included or excluded across repeated evaluations.

## Next Steps

- [Theory: Determinism Proofs](/theory/determinism-proofs) - Review formal determinism guarantees.
- [Load First Snapshot Safely](/quickstart/load-first-snapshot-safely) - Move rollout definitions into runtime JSON.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-10A | Ramp-up assignment is deterministic and exposed through stable bucket APIs. |
