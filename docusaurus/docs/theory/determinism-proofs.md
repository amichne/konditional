# Determinism Proofs

Why same inputs always produce same outputs: SHA-256 bucketing, stable rule ordering, and immutable snapshots.

---

## The Determinism Guarantee

**Claim:** Given the same configuration snapshot and the same context, evaluation always produces the same value.

**Formally:**
```
∀ context C, configuration S:
  evaluate(feature, C, S) = v₁ ∧ evaluate(feature, C, S) = v₂ → v₁ = v₂
```

---

## Mechanism 1: SHA-256 Deterministic Hashing

### Ramp-Up Bucketing

```kotlin
input = "$salt:$flagKey:${stableIdHex}"
hash = sha256(input)
bucket = uint32(hash[0..3]) % 10_000
```

**SHA-256 properties:**
1. **Deterministic** — Same input always produces same hash
2. **Collision-resistant** — Different inputs produce different hashes (with negligible probability of collision)
3. **Uniform distribution** — Hash outputs are uniformly distributed

### Proof of Determinism

**Lemma:** SHA-256 is a deterministic function.

**Proof:** SHA-256 is defined by FIPS 180-4. For any input `m`, `SHA256(m)` is computed via a fixed sequence of operations (message padding, block processing, compression). The algorithm is deterministic (no randomness, no nondeterministic steps).

**Corollary:** For fixed `(salt, flagKey, stableIdHex)`, the bucket assignment is deterministic.

```
bucket(salt, flagKey, stableId) = uint32(SHA256("$salt:$flagKey:${stableId.hex}")[0..3]) % 10_000
```

Since SHA-256 is deterministic, `bucket(...)` is deterministic.

---

## Mechanism 2: Stable Rule Ordering

### Specificity Calculation

```kotlin
specificity(rule) =
  (if platforms.nonEmpty then 1 else 0) +
  (if locales.nonEmpty then 1 else 0) +
  (if versionRange.hasBounds then 1 else 0) +
  axes.size +
  extensionSpecificity
```

**Property:** Specificity is a pure function of rule criteria (no side effects, no randomness).

### Rule Sorting

Rules are sorted by:
1. **Descending specificity** (higher specificity first)
2. **Definition order** (tie-breaker)

**Proof of Stability:**

```kotlin
val sortedRules = rules.sortedByDescending { it.specificity }
```

Kotlin's `sortedByDescending` is a **stable sort**:
- Elements with equal specificity retain their original order
- Sorting is deterministic (same input → same output)

**Corollary:** Rule iteration order is deterministic for a fixed configuration.

---

## Mechanism 3: Immutable Configuration Snapshots

### Configuration Immutability

```kotlin
data class Configuration internal constructor(
    val flags: Map<Feature<*, *, *>, FlagDefinition<*, *, *>>,
    val metadata: ConfigurationMetadata = ConfigurationMetadata(),
)
```

`Configuration` is a Kotlin `data class` with `val` properties:
- All fields are immutable references
- The public API exposes a read-only `Map` (treat the snapshot as immutable)
- No supported mutation methods

**Property:** Once created, a `Configuration` cannot change.

### Evaluation with Snapshot

```kotlin
operator fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.invoke(
    context: C,
    registry: NamespaceRegistry,
): T {
    val config = registry.configuration  // Read current snapshot
    // ... evaluate using config ...
}
```

**Key insight:** Evaluation reads a **snapshot** of configuration at a single point in time. Even if the active configuration changes during evaluation, the snapshot remains immutable.

**Corollary:** For a fixed context and snapshot, evaluation is deterministic.

---

## Putting It Together: Full Determinism Proof

### Theorem

**Given:**
- A feature `f` with default `d` and rules `R = [r₁, r₂, ..., rₙ]`
- A context `C` with `(locale, platform, version, stableId)`
- A configuration snapshot `S` containing `f`'s definition

**Then:**
`evaluate(f, C, S)` is deterministic.

### Proof

**Case 1: Namespace disabled via kill-switch**
- `disableAll()` sets a boolean flag
- Evaluation returns `d` (default)
- Boolean flag is deterministic
- **Result:** deterministic

**Case 2: Flag inactive**
- `isActive` is a boolean in `FlagDefinition`
- Evaluation returns `d` (default)
- **Result:** deterministic

**Case 3: Rules evaluated**

1. **Sort rules by specificity** (stable sort, deterministic)
2. **Iterate rules in order:**
   - **Criteria matching** — Pure functions of `C` (no side effects)
   - **Ramp-up check** — SHA-256 bucketing (deterministic, proven above)
   - **Allowlist check** — Set membership (deterministic)
3. **First matching rule** — Iteration order is deterministic, so first match is deterministic
4. **Return rule value or default** — Both are constants from `S`

**Result:** All steps are deterministic, so final value is deterministic.

**QED.**

---

## What Could Break Determinism (and What Can't)

### ✓ Deterministic: Same Context

```kotlin
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 1, 0),
    stableId = StableId.of("user-123")
)

val v1 = AppFeatures.darkMode(ctx)
val v2 = AppFeatures.darkMode(ctx)
// v1 == v2 (guaranteed)
```

### ✓ Deterministic: Same StableId, Different Context Fields

```kotlin
val ctx1 = Context(..., stableId = StableId.of("user-123"))
val ctx2 = Context(..., stableId = StableId.of("user-123"))

// If same rules match, ramp-up bucket is same (same stableId)
```

### ✗ Non-Deterministic: Different StableId

```kotlin
val ctx1 = Context(..., stableId = StableId.of("user-123"))
val ctx2 = Context(..., stableId = StableId.of("user-456"))

// Different stableId → different bucket → possibly different value
```

This is **intentional**: ramp-up bucketing is deterministic **per user**, not across users.

### ✗ Non-Deterministic: Configuration Changes Between Evaluations

```kotlin
val v1 = AppFeatures.darkMode(ctx)

// Configuration changes
AppFeatures.load(newConfig)

val v2 = AppFeatures.darkMode(ctx)
// v1 might ≠ v2 (config changed)
```

This is **intentional**: determinism is scoped to a single configuration snapshot.

### ✓ Deterministic: Concurrent Evaluations (Same Snapshot)

```kotlin
// Thread 1
val v1 = AppFeatures.darkMode(ctx)

// Thread 2 (before config update)
val v2 = AppFeatures.darkMode(ctx)

// v1 == v2 (both read same snapshot)
```

---

## Testing Determinism

### Unit Test: Same Context → Same Value

```kotlin
@Test
fun `evaluation is deterministic`() {
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-123")
    )

    val results = (1..1000).map {
        AppFeatures.darkMode(ctx)
    }

    assertEquals(1, results.distinct().size, "Non-deterministic evaluation!")
}
```

### Statistical Test: Ramp-Up Distribution

```kotlin
@Test
fun `50 percent ramp-up distributes evenly`() {
    val sampleSize = 10_000
    val enabled = (0 until sampleSize).count { i ->
        val ctx = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(2, 1, 0),
            stableId = StableId.of(i.toString().padStart(32, '0'))
        )
        AppFeatures.rampUpFlag(ctx)
    }

    val percentage = (enabled.toDouble() / sampleSize) * 100
    assertTrue(percentage in 48.0..52.0, "Ramp-up distribution is skewed!")
}
```

---

## Formal Properties Summary

| Property | Mechanism | Guarantee |
|----------|-----------|-----------|
| **Ramp-up determinism** | SHA-256 (FIPS 180-4) | Same `(stableId, flagKey, salt)` → same bucket |
| **Rule ordering** | Stable sort by specificity | Same rules → same iteration order |
| **Snapshot immutability** | Kotlin immutable data classes | Snapshot cannot change after creation |
| **Atomic reads** | `AtomicReference.get()` | Readers see consistent snapshot |
| **Overall determinism** | Composition of above | Same `(context, snapshot)` → same value |

---

## Next Steps

- [Fundamentals: Evaluation Semantics](/fundamentals/evaluation-semantics) — Practical determinism
- [Rules & Targeting: Rollout Strategies](/rules-and-targeting/rollout-strategies) — SHA-256 bucketing
- [Theory: Atomicity Guarantees](/theory/atomicity-guarantees) — Snapshot consistency
