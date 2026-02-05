---
title: RampUpBucketing API
---

# RampUpBucketing API

## What It Does

`RampUpBucketing` provides deterministic user-to-bucket assignment for percentage-based rollouts using SHA-256 hashing. It's the mechanism that ensures the same user always gets the same ramp-up result.

## calculateBucket()

Compute deterministic bucket assignment for a user.

### Signature

```kotlin
object RampUpBucketing {
    fun calculateBucket(
        stableId: StableId,
        featureKey: String,
        salt: String
    ): Int
}
```

**Evidence**: `konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13`

### Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `stableId` | `StableId` | Yes | User's stable identifier (persistent across evaluations) |
| `featureKey` | `String` | Yes | Feature's unique key (typically namespace + feature name) |
| `salt` | `String` | Yes | Salt value for this feature (defaults to `"default"` if not specified) |

### Return Value

Returns `Int` in range `[0, 9999]` (10,000 buckets):
- Deterministic: same inputs → same bucket
- Uniform distribution: each bucket equally likely
- Independent: different features have independent buckets

### Errors / Failure Modes

- **No errors**: Always returns valid bucket number
- **Thread-safe**: Pure function, safe from any thread
- **No exceptions**: Cannot fail (all inputs valid)

### Examples

**Minimal**:
```kotlin
val bucket = RampUpBucketing.calculateBucket(
    stableId = StableId.of("user-123"),
    featureKey = "feature::app::darkMode",
    salt = "default"
)
// Returns: 4523 (deterministic)
```

**Typical** (check if user is in 25% rollout):
```kotlin
val bucket = RampUpBucketing.calculateBucket(
    stableId = StableId.of(userId),
    featureKey = "feature::app::newCheckout",
    salt = "default"
)

val threshold = (25.0 * 100).toInt() // 2500 basis points
val inRollout = bucket < threshold    // true if bucket in [0, 2499]
```

**Edge case** (different salt = different bucket):
```kotlin
val bucket1 = RampUpBucketing.calculateBucket(
    StableId.of("user-123"), "feature::app::darkMode", "v1"
)
// Returns: 4523

val bucket2 = RampUpBucketing.calculateBucket(
    StableId.of("user-123"), "feature::app::darkMode", "v2"  // Different salt
)
// Returns: 8172 (different bucket!)
```

### Semantics / Notes

- **Mechanism**: SHA-256 hash of `"$salt:$featureKey:${stableId.hexId.id}"` reduced to bucket
- **Determinism**: Same inputs always produce same output (no randomness)
- **Independence**: Different features/salts produce independent buckets
- **Uniformity**: Each bucket has ~equal probability (0.01%)
- **Stability**: Bucket assignment stable across app restarts, deployments

### Compatibility

- **Introduced**: v0.1.0
- **Deprecated**: None
- **Alternatives**: None (only way to compute buckets)

---

## Bucketing Algorithm

### Step-by-Step

1. **Construct input string**:
   ```
   "$salt:$featureKey:${stableId.hexId.id}"
   ```

2. **Hash with SHA-256**:
   ```kotlin
   val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(UTF_8))
   ```

3. **Extract first 4 bytes as unsigned int**:
   ```kotlin
   val hashInt = (hash[0].toInt() and 0xFF) shl 24 or
                 (hash[1].toInt() and 0xFF) shl 16 or
                 (hash[2].toInt() and 0xFF) shl 8 or
                 (hash[3].toInt() and 0xFF)
   ```

4. **Reduce to bucket**:
   ```kotlin
   val bucket = (hashInt and 0x7FFFFFFF) % 10_000
   ```

### Properties

**Guarantee**: Same `(stableId, featureKey, salt)` → same bucket.

**Mechanism**: Cryptographic hash function (SHA-256) ensures determinism and uniform distribution.

**Boundary**: Changing any input (stableId, featureKey, salt) changes bucket assignment.

---

## Usage Patterns

### Pattern: Testing Specific Buckets

```kotlin
@Test
fun `verify user is in treatment group`() {
    val userId = "VIP-user-789"
    val bucket = RampUpBucketing.calculateBucket(
        stableId = StableId.of(userId),
        featureKey = "feature::app::experimentalFeature",
        salt = "default"
    )

    // VIP user should be in first 50% (buckets 0-4999)
    assertTrue(bucket < 5000, "VIP user bucket=$bucket should be < 5000")
}
```

### Pattern: Distribution Analysis

```kotlin
@Test
fun `verify uniform distribution`() {
    val buckets = (0 until 10_000).map { i ->
        RampUpBucketing.calculateBucket(
            stableId = StableId.of("user-$i"),
            featureKey = "feature::app::feature",
            salt = "default"
        )
    }

    // Each bucket should appear ~once (uniform distribution)
    val bucketCounts = buckets.groupingBy { it }.eachCount()
    bucketCounts.values.forEach { count ->
        assertTrue(count in 0..3, "Bucket should appear 0-3 times in 10K sample")
    }
}
```

### Pattern: Debugging Bucketing

```kotlin
fun debugBucketing(userId: String, featureKey: String, salt: String) {
    val stableId = StableId.of(userId)
    val bucket = RampUpBucketing.calculateBucket(stableId, featureKey, salt)

    println("""
        Bucketing Debug:
        User ID: $userId
        StableId hex: ${stableId.hexId.id}
        Feature: $featureKey
        Salt: $salt
        Bucket: $bucket (out of 10,000)

        Rollout Coverage:
        10%: ${if (bucket < 1000) "IN" else "OUT"}
        25%: ${if (bucket < 2500) "IN" else "OUT"}
        50%: ${if (bucket < 5000) "IN" else "OUT"}
        75%: ${if (bucket < 7500) "IN" else "OUT"}
    """.trimIndent())
}
```

---

## Mathematical Properties

### Uniformity

Each bucket has probability `1/10_000 = 0.01%`:
- SHA-256 produces uniform output distribution
- Modulo operation preserves uniformity for large hash space
- 10,000 buckets chosen for clean percentage mapping (1% = 100 buckets)

### Independence

Buckets for different features are independent:
- `featureKey` in hash input ensures independence
- Changing any input bit changes ~50% of output bits (avalanche effect)

### Stability Under Percentage Increase

Increasing rollout percentage only adds users:
- 10% → 25%: buckets [0, 999] stay in, buckets [1000, 2499] added
- Users in first 10% remain in rollout (stable subset)

### Instability Under Salt Change

Changing salt redistributes ALL users:
- Different salt → different hash → different bucket
- Use only when intentionally resampling population

---

## Testing Utilities

### TargetingIds Helper

Use `TargetingIds` from testFixtures to find users in specific buckets:

```kotlin
import io.amichne.konditional.fixtures.utilities.TargetingIds

@Test
fun `test with known bucket assignments`() {
    val inBucket = TargetingIds.idInBucket(percentage = 25.0)
    val outBucket = TargetingIds.idOutOfBucket(percentage = 25.0)

    val inCtx = Context(stableId = StableId.of(inBucket))
    val outCtx = Context(stableId = StableId.of(outBucket))

    assertTrue(AppFeatures.feature.evaluate(inCtx))   // In 25%
    assertFalse(AppFeatures.feature.evaluate(outCtx)) // Out of 25%
}
```

---

## Related

- [Guide: Roll Out Gradually](/rollouts-and-bucketing) — Using ramp-ups in practice
- [Guide: Test Features](/installation#test-fixtures-optional) — Testing bucketing behavior
- [Design Theory: Determinism Proofs](/theory/determinism-proofs) — Mathematical proof of determinism
- [Learn: Evaluation Model](/evaluation-flow) — How ramp-ups fit into evaluation
- [Troubleshooting: Bucketing Issues](/troubleshooting#bucketing-issues) — Common problems
