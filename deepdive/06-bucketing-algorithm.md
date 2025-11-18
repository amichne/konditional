# Engineering Deep Dive: Bucketing Algorithm

**Navigate**: [← Previous: Rules & Specificity](05-rules-specificity.md) | [Next: Concurrency Model →](07-concurrency-model.md)

---

## Deterministic Rollouts

A core feature of any feature flag system is gradual rollouts: "Enable this feature for 10% of users." But which 10%? And how do we ensure the same users stay in that 10% across sessions?

This chapter reveals Konditional's bucketing algorithm and why it uses SHA-256 hashing.

## The Bucketing Problem

### Requirements

A good bucketing algorithm must be:

1. **Deterministic**: Same user always gets same bucket
2. **Uniform**: Users evenly distributed across buckets
3. **Stable**: Bucket assignment persists across sessions/devices
4. **Platform-independent**: Same result on iOS, Android, server, etc.
5. **Unpredictable**: Users can't guess their bucket assignment
6. **Controllable**: Can re-shuffle assignments when needed

### Why Not Simple Modulo?

```kotlin
// Naive approach
fun bucket(userId: String, numBuckets: Int): Int =
    userId.hashCode() % numBuckets
```

**Problems**:
1. `hashCode()` is **platform-dependent** (JVM ≠ iOS ≠ Android)
2. Distribution may be **non-uniform** (clustering in certain buckets)
3. Negative values need handling (`hashCode()` can be negative)
4. No control over **re-shuffling** assignments

---

## The stableBucket() Function

Here's the complete implementation from `FlagDefinition.kt`:

```kotlin
private fun stableBucket(
    flagKey: String,
    id: HexId,
    salt: String,
): Int =
    with(shaDigestSpi.digest("$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8))) {
        (
            (
                get(0).toInt() and 0xFF shl 24 or
                    (get(1).toInt() and 0xFF shl 16) or
                    (get(2).toInt() and 0xFF shl 8) or
                    (get(3).toInt() and 0xFF)
                ).toLong() and 0xFFFF_FFFFL
            ).mod(10_000L).toInt()
    }
```

Let's break this down step by step.

---

## Step 1: Input Composition

```kotlin
"$salt:$flagKey:${id.id}".toByteArray(Charsets.UTF_8)
```

### The Input String

Three components concatenated with colons:

```
salt:flagKey:stableId
```

**Example**:
```
"v1:NEW_CHECKOUT:a1b2c3d4e5f6..."
```

### Component Roles

#### 1. salt: String

**Purpose**: Version identifier for re-shuffling assignments

**Default**: `"v1"`

**Effect**: Changing salt redistributes users to different buckets

**Example**:
```kotlin
// Same user, same flag, different salts → different buckets
stableBucket("FEATURE", userId, "v1")  // → 3456
stableBucket("FEATURE", userId, "v2")  // → 7890
```

**When to change**:
- Detected bias in rollout (some buckets have more power users)
- Need to reset rollout (start fresh experiment)
- Discovered bug affecting certain buckets

#### 2. flagKey: String

**Purpose**: Ensures different flags bucket independently

**Effect**: Same user gets different buckets for different flags

**Example**:
```kotlin
// Same user, same salt, different flags → different buckets
stableBucket("FEATURE_A", userId, "v1")  // → 3456
stableBucket("FEATURE_B", userId, "v1")  // → 7890
```

**Why**: Prevents correlation between flags. User in 10% of Feature A isn't necessarily in 10% of Feature B.

#### 3. stableId: String

**Purpose**: Unique, persistent identifier for this user/entity

**Requirements**:
- Minimum 32 hex characters (16 bytes)
- Persistent across sessions
- Unique per user/device/entity

**Example**:
```kotlin
StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
```

**Common sources**:
- User ID (if authenticated)
- Device ID (if anonymous)
- Session ID (if transient bucketing is acceptable)

### UTF-8 Encoding

```kotlin
.toByteArray(Charsets.UTF_8)
```

**Purpose**: Convert string to bytes for hashing

**Why UTF-8**: Platform-independent encoding ensures same bytes on all platforms.

---

## Step 2: SHA-256 Hashing

```kotlin
shaDigestSpi.digest(...)
```

### What is SHA-256?

**SHA-256** (Secure Hash Algorithm 256-bit) is a cryptographic hash function.

**Properties**:
- **Deterministic**: Same input → same output
- **Uniform**: Output evenly distributed across 2^256 space
- **Avalanche effect**: Small input change → completely different output
- **Platform-independent**: Same result on all platforms (defined by specification)

### Why SHA-256?

#### Alternative 1: hashCode()

```kotlin
"$salt:$flagKey:$id".hashCode()
```

**Problems**:
- Platform-dependent (JVM ≠ JS ≠ Native)
- Non-uniform distribution
- Only 32 bits of output
- No standardization

#### Alternative 2: MurmurHash or XXHash

Fast non-cryptographic hashes.

**Problems**:
- Need to implement or import library
- Less well-specified than SHA-256
- Overkill (we only need 32 bits)

#### Why SHA-256?

**Advantages**:
1. **Built-in**: Available on all platforms (JVM, iOS, Android, Web)
2. **Standardized**: FIPS 180-4 specification ensures consistency
3. **Uniform**: Excellent distribution properties
4. **Sufficient entropy**: 256 bits >> 14 bits needed (10,000 buckets)
5. **Well-tested**: Cryptographic properties thoroughly analyzed

**Trade-offs**:
- Slower than non-cryptographic hashes (but fast enough for this use case)
- Overkill (256 bits when we only need ~14 bits)

**Decision**: Consistency and platform-independence outweigh performance cost.

### The Digest

```kotlin
shaDigestSpi.digest(...)
```

Returns a `ByteArray` of 32 bytes (256 bits).

**Example output** (first 4 bytes shown):
```
[0xA1, 0xB2, 0xC3, 0xD4, ...]
```

---

## Step 3: Extract 32-bit Integer

```kotlin
get(0).toInt() and 0xFF shl 24 or
    (get(1).toInt() and 0xFF shl 16) or
    (get(2).toInt() and 0xFF shl 8) or
    (get(3).toInt() and 0xFF)
```

### Why Only First 4 Bytes?

SHA-256 produces 32 bytes, but we only need ~14 bits for 10,000 buckets.

**Using first 4 bytes** (32 bits):
- More than enough entropy
- Uniform distribution preserved
- Simpler code

### Byte-to-Int Conversion

Each byte operation:
```kotlin
get(i).toInt() and 0xFF shl (24 - i * 8)
```

#### Step-by-step

Assume first 4 bytes are `[0xA1, 0xB2, 0xC3, 0xD4]`:

**Byte 0**:
```kotlin
get(0).toInt() and 0xFF shl 24
// → 0xA1 and 0xFF shl 24
// → 0xA1 shl 24
// → 0xA1000000
```

**Byte 1**:
```kotlin
get(1).toInt() and 0xFF shl 16
// → 0xB2 and 0xFF shl 16
// → 0xB2 shl 16
// → 0x00B20000
```

**Byte 2**:
```kotlin
get(2).toInt() and 0xFF shl 8
// → 0xC3 and 0xFF shl 8
// → 0xC3 shl 8
// → 0x0000C300
```

**Byte 3**:
```kotlin
get(3).toInt() and 0xFF
// → 0xD4 and 0xFF
// → 0x000000D4
```

**Combine with OR**:
```kotlin
0xA1000000 or 0x00B20000 or 0x0000C300 or 0x000000D4
// → 0xA1B2C3D4
```

**Result**: 32-bit integer from first 4 bytes.

### Why `and 0xFF`?

**Problem**: `Byte` is signed in JVM (-128 to 127).

**Example**:
```kotlin
val b: Byte = 0xA1.toByte()  // → -95 (negative!)
b.toInt()  // → -95, not 0xA1
```

**Solution**: Mask with `0xFF` to keep only lower 8 bits:
```kotlin
b.toInt() and 0xFF  // → 0xA1 (positive)
```

This ensures we extract the byte value as unsigned.

---

## Step 4: Ensure Positive

```kotlin
.toLong() and 0xFFFF_FFFFL
```

### Why Convert to Long?

**Problem**: The 32-bit integer might be negative (sign bit set).

**Example**:
```
0xA1B2C3D4 as Int → -1585512492 (negative because bit 31 is 1)
```

**Solution**: Convert to `Long` and mask with `0xFFFF_FFFFL`:
```kotlin
0xA1B2C3D4.toLong() and 0xFFFF_FFFFL
// → 0x00000000A1B2C3D4 (positive Long)
```

**Result**: Non-negative Long with same bit pattern.

---

## Step 5: Modulo 10,000

```kotlin
.mod(10_000L).toInt()
```

### Why 10,000 Buckets?

**Range**: 0 to 9,999

**Granularity**: 0.01% (10,000 buckets = 100.00%)

**Supports**:
- Rollout percentages with 2 decimal places (e.g., 12.34%)
- Fine-grained control
- Standard convention in feature flag systems

### Modulo Operation

```kotlin
.mod(10_000L)
```

**Maps**: Large positive Long → integer in [0, 9999]

**Distribution**: Uniform if input is uniform (SHA-256 ensures this)

**Example**:
```kotlin
2718281828.mod(10_000)  // → 1828
3141592653.mod(10_000)  // → 2653
```

### Convert Back to Int

```kotlin
.toInt()
```

Safe because result is in [0, 9999], well within `Int` range.

---

## Rollout Threshold Comparison

The bucketing result is compared to a threshold in `isInEligibleSegment()`:

```kotlin
private fun isInEligibleSegment(
    flagKey: String,
    id: HexId,
    salt: String,
    rollout: Rollout,
): Boolean =
    when {
        rollout <= 0.0 -> false
        rollout >= 100.0 -> true
        else -> stableBucket(flagKey, id, salt) < (rollout.value * 100).roundToInt()
    }
```

### Edge Cases: 0% and 100%

```kotlin
rollout <= 0.0 -> false
rollout >= 100.0 -> true
```

**Optimization**: Skip hashing for 0% and 100% rollouts.

**Why**: Common cases (disabled or fully enabled) don't need bucketing.

### Threshold Calculation

```kotlin
(rollout.value * 100).roundToInt()
```

**Rollout percentage** → **Bucket threshold**

**Examples**:
- 10.0% → 1000
- 50.0% → 5000
- 12.34% → 1234

### Comparison

```kotlin
stableBucket(flagKey, id, salt) < threshold
```

**Bucket in [0, 9999]**
**Threshold in [0, 10000]**

**Logic**: If bucket < threshold, user is in rollout.

**Examples**:

**50% rollout** (threshold = 5000):
- Bucket 3456 < 5000 ✓ → In rollout
- Bucket 7890 < 5000 ✗ → Not in rollout

**10% rollout** (threshold = 1000):
- Bucket 123 < 1000 ✓ → In rollout
- Bucket 1234 < 1000 ✗ → Not in rollout

---

## Bucketing Properties

### Property 1: Deterministic

**Same inputs → Same output**

```kotlin
val bucket1 = stableBucket("FEATURE", userId, "v1")
val bucket2 = stableBucket("FEATURE", userId, "v1")
// bucket1 == bucket2 (always)
```

**Why**: SHA-256 is deterministic.

**Benefit**: User gets consistent experience across sessions.

### Property 2: Uniform Distribution

**All buckets equally likely**

```kotlin
// Given random userIds, each bucket [0, 9999] has ~equal count
val buckets = userIds.map { stableBucket("FEATURE", it, "v1") }
val distribution = buckets.groupingBy { it }.eachCount()
// distribution[0] ≈ distribution[1] ≈ ... ≈ distribution[9999]
```

**Why**: SHA-256 has excellent uniformity properties.

**Benefit**: Rollout percentages are accurate. 10% rollout ≈ 10% of users.

### Property 3: Avalanche Effect

**Small input change → Completely different output**

```kotlin
stableBucket("FEATURE", "user123", "v1")  // → 3456
stableBucket("FEATURE", "user124", "v1")  // → 7890 (very different)
```

**Why**: SHA-256 avalanche property.

**Benefit**: Adjacent user IDs get different buckets (no clustering).

### Property 4: Independence Across Flags

**Same user, different flags → Different buckets**

```kotlin
stableBucket("FEATURE_A", userId, "v1")  // → 3456
stableBucket("FEATURE_B", userId, "v1")  // → 7890
```

**Why**: Flag key is part of hash input.

**Benefit**: User's inclusion in one rollout doesn't predict inclusion in another.

### Property 5: Salt-based Re-shuffling

**Same user, different salts → Different buckets**

```kotlin
stableBucket("FEATURE", userId, "v1")  // → 3456
stableBucket("FEATURE", userId, "v2")  // → 7890
```

**Why**: Salt is part of hash input.

**Benefit**: Can redistribute users when needed.

---

## Salt: The Reshuffling Mechanism

### What is Salt?

A version string that changes bucket assignments.

**Default**: `"v1"`

**Effect**: Changing salt redistributes all users.

### When to Change Salt

#### Scenario 1: Detected Bias

You notice that the 10% rollout has disproportionately more power users.

**Action**: Change salt from `"v1"` to `"v2"`
**Result**: Different 10% of users, hopefully more representative

#### Scenario 2: Experiment Reset

You ran an experiment, now want to run a new one with fresh assignments.

**Action**: Increment salt
**Result**: New random sample

#### Scenario 3: Bug Workaround

A bug affects users in buckets 5000-5999.

**Action**: Change salt to redistribute users
**Result**: Affected users likely get different buckets

### How to Change Salt

```kotlin
val definition = FlagDefinition(
    feature = MY_FEATURE,
    defaultValue = false,
    values = listOf(/* rules */),
    salt = "v2"  // ← Changed from "v1"
)
```

**Effect**: All bucket calculations for this flag now use `"v2"`.

### Salt Strategies

#### Strategy 1: Global Versioning

All flags use same salt version:
```kotlin
const val CURRENT_SALT = "v3"
```

**Pros**: Simple, consistent
**Cons**: All flags reshuffled together

#### Strategy 2: Per-Flag Versioning

Each flag has its own salt:
```kotlin
val FEATURE_A = FlagDefinition(..., salt = "feature_a_v2")
val FEATURE_B = FlagDefinition(..., salt = "feature_b_v1")
```

**Pros**: Independent control
**Cons**: More management overhead

#### Strategy 3: Timestamp-based

Use timestamp as salt for experiments:
```kotlin
salt = "exp_2024_01_15"
```

**Pros**: Self-documenting
**Cons**: Harder to reason about

**Recommendation**: Use simple versioning (`"v1"`, `"v2"`, ...) unless you have specific needs.

---

## Bucketing Example

Let's trace a complete bucketing operation:

### Inputs

```kotlin
flagKey = "NEW_CHECKOUT"
stableId = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
salt = "v1"
rollout = 50.0
```

### Step 1: Compose Input

```kotlin
input = "v1:NEW_CHECKOUT:a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
```

### Step 2: Hash with SHA-256

```kotlin
bytes = SHA256(input.toByteArray(UTF_8))
// → [0x7A, 0x1B, 0x3C, 0x8D, ...] (32 bytes)
```

### Step 3: Extract First 4 Bytes

```kotlin
value =
    (0x7A shl 24) or
    (0x1B shl 16) or
    (0x3C shl 8) or
    (0x8D)
// → 0x7A1B3C8D
```

### Step 4: Ensure Positive

```kotlin
value = 0x7A1B3C8D.toLong() and 0xFFFF_FFFFL
// → 2048564365
```

### Step 5: Modulo 10,000

```kotlin
bucket = 2048564365.mod(10_000)
// → 4365
```

### Step 6: Compare to Threshold

```kotlin
threshold = (50.0 * 100).roundToInt()
// → 5000

isInRollout = bucket < threshold
// → 4365 < 5000
// → true ✓
```

**Result**: This user is in the 50% rollout.

---

## Platform Independence

### Why It Matters

Users interact with your application on multiple platforms:
- Mobile app (iOS, Android)
- Web app (browser)
- Backend API (server)

**Requirement**: Same user ID should bucket identically on all platforms.

### How SHA-256 Ensures This

SHA-256 is **fully specified** by FIPS 180-4:
- Exact algorithm defined
- Same input → same output on any compliant implementation
- Available in standard libraries on all platforms

**Example**:

**iOS** (Swift):
```swift
let input = "v1:FEATURE:user123"
let hash = SHA256.hash(data: input.data(using: .utf8)!)
```

**Android** (Kotlin):
```kotlin
val input = "v1:FEATURE:user123"
val hash = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
```

**JavaScript** (Web Crypto API):
```javascript
const input = "v1:FEATURE:user123";
const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
```

**All produce identical hash.**

### Testing Platform Independence

```kotlin
@Test
fun `bucket calculation matches across platforms`() {
    val expectedBucket = 4365  // Pre-computed on reference platform

    val bucket = stableBucket(
        flagKey = "NEW_CHECKOUT",
        id = HexId("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"),
        salt = "v1"
    )

    assertEquals(expectedBucket, bucket)
}
```

Run this test on iOS, Android, JVM, JS. All should pass.

---

## Performance Considerations

### Is SHA-256 Too Slow?

**Typical performance**: ~1 microsecond per hash on modern hardware

**Evaluation flow**:
1. Rule matching: ~10 nanoseconds (simple boolean checks)
2. SHA-256 hash: ~1 microsecond (if rule matches)
3. Bucket calculation: ~10 nanoseconds (arithmetic)

**Bottleneck**: SHA-256, but still very fast in absolute terms.

### Optimization: Short-Circuit

Recall from evaluation engine:

```kotlin
it.rule.matches(context) && isInEligibleSegment(...)
```

**Short-circuit**: If `matches()` returns false, hashing skipped.

**Impact**: Only compute hash for matching rules (typically 0-1 per evaluation).

### Optimization: 0% and 100% Rollouts

```kotlin
when {
    rollout <= 0.0 -> false
    rollout >= 100.0 -> true
    else -> stableBucket(...) < threshold
}
```

**Impact**: Common cases (fully disabled/enabled) skip hashing entirely.

### Benchmark

**Evaluation with rollout** (matching rule):
- ~1-2 microseconds total
- Dominated by SHA-256

**Evaluation without rollout** (no matching rule):
- ~100 nanoseconds total
- No hashing needed

**Conclusion**: SHA-256 is the "expensive" part, but still fast enough for real-time evaluation.

---

## Bucketing Best Practices

### 1. Use Persistent StableIds

```kotlin
// ✓ Good: User ID (persistent)
StableId.of(user.id.toString())

// ✓ Good: Device ID (persistent)
StableId.of(deviceId)

// ✗ Bad: Session ID (changes each session)
StableId.of(sessionId)

// ✗ Bad: Random value (changes each evaluation)
StableId.of(UUID.randomUUID().toString())
```

**Why**: Determinism requires persistent identifiers.

### 2. Ensure Sufficient Entropy

```kotlin
// ✓ Good: 32+ hex characters
StableId.of("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")

// ✗ Bad: Too short (will be rejected)
StableId.of("123")
```

**Why**: Short IDs reduce entropy, risk non-uniform distribution.

### 3. Don't Correlate StableIds with Behavior

```kotlin
// ✗ Bad: Sequential user IDs from database
// user 1, user 2, user 3, ...
// If first 1000 users are test accounts, 10% rollout might catch them all
```

**Mitigation**: Use UUIDs or hash database IDs before using as StableId.

### 4. Version Salts When Reshuffling

```kotlin
// ✓ Good: Clear versioning
salt = "v1"
salt = "v2"
salt = "v3"

// ✗ Bad: Unclear or too specific
salt = "tuesday_experiment"
```

**Why**: Simple versions are easier to track and reason about.

### 5. Document Salt Changes

```kotlin
// ✓ Good: Comment why salt changed
val FLAG = FlagDefinition(
    ...,
    salt = "v2"  // Changed from v1 on 2024-01-15 to fix bias toward power users
)
```

**Why**: Future maintainers understand why users were reshuffled.

---

## Review: Bucketing Algorithm

### The Algorithm

```kotlin
stableBucket(flagKey, userId, salt) =
    SHA256("$salt:$flagKey:$userId")
        .take(4 bytes)
        .asUInt32()
        .mod(10_000)
```

### Why SHA-256?

- Deterministic
- Uniform distribution
- Platform-independent
- Built-in on all platforms
- Well-specified and tested

### Key Properties

1. **Deterministic**: Same user → same bucket
2. **Uniform**: Users evenly distributed
3. **Independent**: Different flags → different buckets
4. **Reshuffle-able**: Salt change → new distribution
5. **Platform-independent**: Same result everywhere

### The Rollout Check

```kotlin
bucket < (rollout * 100).roundToInt()
```

Maps rollout percentage to bucket threshold.

---

## Next Steps

Now that you understand how bucketing works, we can explore how Konditional handles concurrent access safely.

**Next chapter**: [Concurrency Model](07-concurrency-model.md)
- Immutable data structures
- AtomicReference in NamespaceRegistry
- Lock-free reads
- Atomic configuration updates
- Thread safety guarantees

Immutability and atomic references make thread-safety simple and efficient. Let's see how.

---

**Navigate**: [← Previous: Rules & Specificity](05-rules-specificity.md) | [Next: Concurrency Model →](07-concurrency-model.md)
