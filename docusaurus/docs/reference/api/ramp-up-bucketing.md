---
title: RampUpBucketing API
---

# RampUpBucketing API

This page defines the public deterministic bucketing APIs used for rollout
inspection and debugging.

## Read this page when

- You need to compute a stable rollout bucket for a user.
- You need a full bucket explanation payload for diagnostics.
- You are verifying rollout determinism outside a direct feature evaluation
  call.

## API and contract reference

```kotlin
object RampUpBucketing {
    fun bucket(
        stableId: StableId,
        featureKey: String,
        salt: String,
    ): Int

    fun explain(
        stableId: StableId,
        featureKey: String,
        salt: String,
        rampUp: RampUp,
    ): BucketInfo
}
```

`BucketInfo` payload:

```kotlin
data class BucketInfo internal constructor(
    val featureKey: String,
    val salt: String,
    val bucket: Int,
    val rollout: RampUp,
    val thresholdBasisPoints: Int,
    val inRollout: Boolean,
)
```

Contract:

- `bucket(...)` returns `Int` in `[0, 9999]`.
- `explain(...)` uses the same bucket algorithm as runtime evaluation and
  returns rollout decision details.

## Deterministic API and contract notes

- Bucketing input is deterministic for `(salt, featureKey, stableId.hexId)`.
- Ramp-up inclusion uses deterministic threshold comparison against the computed
  bucket.
- Public bucketing utilities are guaranteed to match core evaluation bucketing
  behavior.

## Canonical conceptual pages

- [Theory: Determinism proofs](/theory/determinism-proofs)
- [How-to: Rolling out gradually](/how-to-guides/rolling-out-gradually)
- [How-to: Debugging determinism](/how-to-guides/debugging-determinism)

## Next steps

- [Feature evaluation API](/reference/api/feature-evaluation)
- [Namespace operations API](/reference/api/namespace-operations)
- [Glossary](/reference/glossary)
