# Evaluation: Deterministic Flag Resolution

This document explains how Konditional evaluates flags, including rule matching, specificity ordering, and rollout bucketing.

---

## The Evaluation Flow

### High-Level Overview

```kotlin
context.evaluate(Features.DARK_MODE)
    ↓
Registry lookup: Get FlagDefinition for DARK_MODE
    ↓
Iterate through rules (sorted by specificity DESC)
    ↓
For each rule:
    - Does rule match context? (platform, locale, version, custom logic)
    - Is context in rollout bucket? (SHA-256 bucketing)
    ↓
Return first matching value, or default if no match
```

### Complete Flow Diagram

```
Context.evaluate(feature)
  |
  +-> FlagRegistry.featureFlag(feature)
  |     |
  |     +-> Returns FlagDefinition<S, T, C>?
  |
  +-> If null: Return EvaluationResult.NotFound
  |
  +-> FlagDefinition.evaluate(context)
        |
        +-> Check isActive
        |     |
        |     +-> If false: Return defaultValue
        |
        +-> Iterate rules (sorted by specificity DESC)
        |     |
        |     +-> For each ConditionalValue:
        |           |
        |           +-> Rule.matches(context)?
        |           |     |
        |           |     +-> BaseEvaluable.matches(context)?
        |           |     |     |
        |           |     |     +-> Platform match?
        |           |     |     +-> Locale match?
        |           |     |     +-> Version match?
        |           |     |
        |           |     +-> Extension.matches(context)?
        |           |           |
        |           |           +-> Custom business logic
        |           |
        |           +-> isInEligibleSegment(context, rollout)?
        |           |     |
        |           |     +-> SHA-256 bucketing
        |           |
        |           +-> If both true: RETURN value
        |
        +-> No match: Return defaultValue
```

---

## Rule Matching

### The AND Logic

For a rule to match, **all criteria must be true**:

```kotlin
config {
    Features.PREMIUM_EXPORT with {
        default(false)

        rule {
            platforms(Platform.IOS)           // Must be iOS
            versions { min(2, 0, 0) }         // AND version >= 2.0.0
            rollout = Rollout.of(50.0)        // AND in 50% bucket
        }.implies(true)
    }
}

// Match requires ALL three:
val context = AppContext(
    platform = Platform.IOS,        // ✓ Match
    appVersion = Version(2, 1, 0),  // ✓ Match (>= 2.0.0)
    stableId = StableId.of("user-123")  // SHA-256 bucket check
)

// If stableId hashes into top 50% bucket:
context.evaluate(Features.PREMIUM_EXPORT)  // Returns: true

// If any condition fails:
val androidContext = context.copy(platform = Platform.ANDROID)
androidContext.evaluate(Features.PREMIUM_EXPORT)  // Returns: false (default)
```

### Platform Matching

```kotlin
rule {
    platforms(Platform.IOS, Platform.ANDROID)
}.implies(value)

// Match if:
context.platform in setOf(Platform.IOS, Platform.ANDROID)

// If platforms() not called:
// Matches ALL platforms (no constraint)
```

### Locale Matching

```kotlin
rule {
    locales(AppLocale.EN_US, AppLocale.EN_GB)
}.implies(value)

// Match if:
context.locale in setOf(AppLocale.EN_US, AppLocale.EN_GB)

// If locales() not called:
// Matches ALL locales (no constraint)
```

### Version Matching

```kotlin
rule {
    versions { min(2, 0, 0) }           // >= 2.0.0
}.implies(value)

rule {
    versions { max(3, 0, 0) }           // < 3.0.0
}.implies(value)

rule {
    versions {
        min(2, 0, 0)
        max(3, 0, 0)
    }                                    // >= 2.0.0 AND < 3.0.0
}.implies(value)

// Match if:
context.appVersion in versionRange

// If versions() not called:
// Matches ALL versions (no constraint)
```

### Custom Extension Matching

```kotlin
rule {
    extension {
        object : Evaluable<AppContext>() {
            override fun matches(context: AppContext): Boolean {
                // Custom business logic
                return context.subscriptionTier == SubscriptionTier.ENTERPRISE &&
                       context.experimentGroups.contains("new-ui")
            }

            override fun specificity(): Int = 1
        }
    }
}.implies(value)

// Match if:
extension.matches(context) == true
```

### Combining Base and Extension

```kotlin
rule {
    // Base targeting
    platforms(Platform.IOS)
    versions { min(2, 0, 0) }

    // Custom extension
    extension {
        object : Evaluable<AppContext>() {
            override fun matches(context: AppContext): Boolean =
                context.subscriptionTier == SubscriptionTier.ENTERPRISE
            override fun specificity(): Int = 1
        }
    }
}.implies(value)

// Match requires:
// context.platform == Platform.IOS
// AND context.appVersion >= Version(2, 0, 0)
// AND context.subscriptionTier == SubscriptionTier.ENTERPRISE
```

---

## Specificity Ordering

### Why Specificity Matters

When multiple rules match, **the most specific rule wins**:

```kotlin
config {
    Features.THEME with {
        default("light")

        // Specificity = 1 (platform only)
        rule {
            platforms(Platform.IOS)
        }.implies("dark-ios")

        // Specificity = 2 (platform + locale)
        rule {
            platforms(Platform.IOS)
            locales(AppLocale.EN_US)
        }.implies("dark-us-ios")
    }
}

// Context: iOS + EN_US
val context = basicContext(
    platform = Platform.IOS,
    locale = AppLocale.EN_US
)

// Both rules match, but specificity = 2 wins
context.evaluate(Features.THEME)  // Returns: "dark-us-ios"
```

### Specificity Calculation

```kotlin
Rule specificity = baseEvaluable.specificity() + extension.specificity()

BaseEvaluable specificity:
  (platforms not empty ? 1 : 0) +
  (locales not empty ? 1 : 0) +
  (version has bounds ? 1 : 0)

Extension specificity:
  Custom evaluable's specificity() method (default: 0)

Total range: 0 to 4+ (base 0-3, extension 0+)
```

### Examples

```kotlin
// Specificity = 0 (no constraints)
rule {
    // No criteria
}.implies("default")

// Specificity = 1 (platform)
rule {
    platforms(Platform.IOS)
}.implies("ios")

// Specificity = 2 (platform + locale)
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
}.implies("ios-us")

// Specificity = 3 (platform + locale + version)
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
    versions { min(2, 0, 0) }
}.implies("ios-us-v2")

// Specificity = 4 (base 3 + extension 1)
rule {
    platforms(Platform.IOS)
    locales(AppLocale.EN_US)
    versions { min(2, 0, 0) }
    extension {
        object : Evaluable<AppContext>() {
            override fun matches(context: AppContext): Boolean =
                context.subscriptionTier == SubscriptionTier.ENTERPRISE
            override fun specificity(): Int = 1
        }
    }
}.implies("ios-us-v2-enterprise")
```

### Tie-Breaking

If two rules have the same specificity, **note text is used for deterministic ordering**:

```kotlin
config {
    Features.THEME with {
        default("light")

        // Both have specificity = 1
        rule {
            platforms(Platform.IOS)
            note("iOS dark theme")  // ← Comes first alphabetically
        }.implies("dark-ios")

        rule {
            locales(AppLocale.EN_US)
            note("US light theme")  // ← Comes second alphabetically
        }.implies("light-us")
    }
}

// If both match, "iOS dark theme" wins (alphabetically first)
```

**Best practice:** Use unique, descriptive notes to ensure deterministic ordering.

### Evaluation Order

Rules are **sorted once at configuration time**:

```kotlin
// Configuration time:
values.sortedWith(
    compareByDescending<ConditionalValue<S, T, C>> { it.rule.specificity() }
        .thenBy { it.rule.note ?: "" }
)

// Evaluation time:
// Iterate through pre-sorted list
// Return first match
```

**Performance:** O(n) where n = number of rules (typically < 10)

---

## Rollout Bucketing

### The Bucketing Algorithm

Konditional uses **SHA-256 hashing** for deterministic bucketing:

```kotlin
fun isInEligibleSegment(
    flagKey: String,
    stableId: StableId,
    salt: String,
    rollout: Rollout
): Boolean {
    if (rollout <= 0.0) return false
    if (rollout >= 100.0) return true

    val bucket = stableBucket(flagKey, stableId, salt)
    return bucket < (rollout.value * 100).toInt()
}

fun stableBucket(
    flagKey: String,
    stableId: StableId,
    salt: String
): Int {
    val hash = SHA256("$salt:$flagKey:${stableId.id}")
    val first4Bytes = hash.take(4).toInt()
    return first4Bytes % 10_000  // Range: 0-9999 (0.01% granularity)
}
```

### Bucketing Properties

**1. Deterministic**
```kotlin
// Same inputs always produce same bucket
val context = basicContext(stableId = StableId.of("user-123"))

context.evaluate(Features.NEW_CHECKOUT)  // Result: true
context.evaluate(Features.NEW_CHECKOUT)  // Result: true (always)
```

**2. Independent Per Flag**
```kotlin
// Flag key is part of hash input
// Each flag has its own bucketing space
SHA256("salt:feature_a:user-123")  // Bucket for feature A
SHA256("salt:feature_b:user-123")  // Bucket for feature B (independent!)

// User in 50% rollout for feature A != user in 50% rollout for feature B
```

**3. Platform-Stable**
```kotlin
// SHA-256 is platform-independent
// Same user gets same bucket on JVM, Android, iOS, web
val jvmBucket = stableBucket("flag", StableId.of("user-123"), "salt")
val androidBucket = stableBucket("flag", StableId.of("user-123"), "salt")
// jvmBucket == androidBucket (guaranteed)
```

**4. Fine-Grained**
```kotlin
// 0-9999 range = 0.01% granularity
rollout = Rollout.of(0.5)   // 0.5% rollout (50 out of 10,000)
rollout = Rollout.of(25.0)  // 25% rollout (2,500 out of 10,000)
rollout = Rollout.of(99.99) // 99.99% rollout (9,999 out of 10,000)
```

**5. Salt-Based Redistribution**
```kotlin
// Changing salt redistributes buckets
config {
    Features.NEW_CHECKOUT with {
        default(false)
        salt("v1")  // Salt version
        rule {
            rollout = Rollout.of(50.0)
        }.implies(true)
    }
}

// If rollout goes wrong, change salt to redistribute
config {
    Features.NEW_CHECKOUT with {
        default(false)
        salt("v2")  // ← New salt = new bucket assignments
        rule {
            rollout = Rollout.of(50.0)
        }.implies(true)
    }
}
```

### Rollout Guarantees

| Property | String-Based Approach | Konditional |
|----------|----------------------|-------------|
| **Deterministic** | hashCode() varies by platform/restart | SHA-256 always same |
| **Independent** | Same hash for all flags = correlation | Separate hash per flag |
| **Stable** | Changes across sessions | Same via stableId |
| **Fine-grained** | Usually % only | 0.01% granularity |
| **Redistributable** | Hard to change | Change salt |

---

## Why This Prevents Errors

### String-Based Evaluation

```kotlin
// Runtime errors possible at every step
val definition = config.getFlag("dark_mode")  // Could be null
val value = definition?.evaluate(context)     // Could be null
val enabled: Boolean = value as Boolean       // Could throw ClassCastException

// Wrong context type: Runtime error
val wrongContext = BasicContext(...)
wrongContext.evaluate("enterprise_feature")  // 💣 Missing required fields
```

### Type-Safe Evaluation

```kotlin
// Type safety at every step
val enabled: Boolean = context.evaluate(Features.DARK_MODE)
//          ↑ Non-null                  ↑ Enum member     ↑ Context type enforced

// Wrong context type: Compile error
val basicContext: Context = basicContext(...)
basicContext.evaluate(EnterpriseFeatures.ANALYTICS)  // ✗ Type mismatch
```

### Evaluation Guarantees

| Guarantee | How Achieved |
|-----------|--------------|
| **Non-null result** | Default value required at compile time |
| **Correct type** | Generic type parameter enforced |
| **Correct context** | Context type parameter enforced |
| **Deterministic** | SHA-256 bucketing, sorted rules |
| **Thread-safe** | Immutable data structures |

---

## Evaluation Performance

### Lookup: O(1)

```kotlin
// HashMap lookup by feature key
val definition = registry.featureFlag(Features.DARK_MODE)
// Time: O(1) average case
```

### Matching: O(n)

```kotlin
// Iterate through rules (sorted by specificity)
for (conditionalValue in sortedValues) {
    if (conditionalValue.rule.matches(context)) {
        return conditionalValue.value
    }
}
// Time: O(n) where n = number of rules
// Typical: n < 10, so effectively constant
```

### Bucketing: O(1)

```kotlin
// SHA-256 hash computation
val bucket = stableBucket(flagKey, stableId, salt)
// Time: O(1) for fixed-length input
```

### Total: O(1) + O(n) + O(1) = O(n)

**In practice:** Near-constant time for typical flag configurations (< 10 rules).

### Memory: Zero Per-Request Allocation

```kotlin
// All data structures immutable and pre-allocated
val definition = registry.featureFlag(feature)  // No allocation
val result = definition.evaluate(context)       // No allocation
```

**Performance characteristics:**
-  No locks (lock-free reads)
-  No allocations (immutable snapshots)
-  Predictable latency (deterministic algorithm)
-  Cache-friendly (sorted data, sequential access)

---

## Testing Evaluation

### Testing Specificity

```kotlin
@Test
fun `most specific rule wins`() {
    val registry = FlagRegistry.create()

    config(registry) {
        Features.THEME with {
            default("light")

            rule {
                platforms(Platform.IOS)
            }.implies("dark-ios")

            rule {
                platforms(Platform.IOS)
                locales(AppLocale.EN_US)
            }.implies("dark-us-ios")
        }
    }

    val context = basicContext(
        platform = Platform.IOS,
        locale = AppLocale.EN_US
    )

    val result = context.evaluate(Features.THEME, registry)

    assertEquals("dark-us-ios", result)  // Most specific wins
}
```

### Testing Rollout Bucketing

```kotlin
@Test
fun `rollout bucketing is deterministic`() {
    val registry = FlagRegistry.create()

    config(registry) {
        Features.NEW_CHECKOUT with {
            default(false)
            rule {
                rollout = Rollout.of(50.0)  // 50% rollout
            }.implies(true)
        }
    }

    val context = basicContext(stableId = StableId.of("user-123"))

    // Evaluate multiple times
    val result1 = context.evaluate(Features.NEW_CHECKOUT, registry)
    val result2 = context.evaluate(Features.NEW_CHECKOUT, registry)
    val result3 = context.evaluate(Features.NEW_CHECKOUT, registry)

    // Same result every time
    assertEquals(result1, result2)
    assertEquals(result2, result3)
}

@Test
fun `rollout percentage approximate`() {
    val registry = FlagRegistry.create()

    config(registry) {
        Features.NEW_CHECKOUT with {
            default(false)
            rule {
                rollout = Rollout.of(50.0)
            }.implies(true)
        }
    }

    // Test with 1000 different users
    val enabled = (1..1000).count { i ->
        val context = basicContext(stableId = StableId.of("user-$i"))
        context.evaluate(Features.NEW_CHECKOUT, registry)
    }

    // Should be approximately 500 (50%)
    assertTrue(enabled in 450..550)  // Allow 10% variance
}
```

---

## Summary: Evaluation Guarantees

| Aspect | Guarantee |
|--------|-----------|
| **Type safety** | Return type matches flag definition, enforced at compile time |
| **Determinism** | Same inputs always produce same output |
| **Specificity** | Most specific matching rule always wins |
| **Bucketing** | SHA-256 ensures independent, stable buckets per flag |
| **Performance** | O(n) where n = rules per flag (typically < 10) |
| **Thread safety** | Lock-free reads, immutable data |
| **Null safety** | Never returns null, default value guaranteed |

**Core Principle:** Evaluation is deterministic, type-safe, and performant.

---

## Next Steps

- **[Registry and Concurrency](./RegistryAndConcurrency.md)** - Thread safety and atomic updates
- **[Core Concepts](./CoreConcepts.md)** - Back to type system fundamentals
- **[Rules Guide](./Rules.md)** - Advanced rule patterns
- **[Context Guide](./Context.md)** - Design evaluation contexts
