---
name: evaluation
description: Explain Konditional's deterministic, non-null evaluation semantics with specificity-based rule precedence and SHA-256 bucketing
---

# Konditional Evaluation

## Instructions

### Core Guarantee
**Evaluation is total**: Every evaluation returns the feature's declared type (never null).

```kotlin
val result: Boolean = Features.darkMode.evaluate(context)
// Always returns Boolean, never Boolean? or throws
```

### Evaluation Flow
1. **Registry lookup** - Find feature by key (O(1))
2. **Namespace check** - If namespace disabled, return default
3. **Feature active check** - If feature inactive, return default
4. **Sort rules** - By specificity (descending)
5. **Iterate rules** - For each rule (most specific first):
   - Check if all criteria match context
   - If criteria don't match → skip to next rule
   - If criteria match → check rollout/allowlist
   - If in rollout OR allowlisted → return rule value
   - Otherwise → skip to next rule
6. **Return default** - If no rules applied

### Specificity
Specificity = number of targeting criteria (not including rollout):

```kotlin
platforms(IOS)                           // specificity: 1
platforms(IOS) + locales(US)             // specificity: 2
platforms(IOS) + locales(US) + versions  // specificity: 3
rollout { 50.0 }                         // specificity: 0
```

**Key point**: Rollout percentage does NOT affect specificity—it only gates whether a matching rule applies.

### Deterministic Bucketing
```kotlin
// SHA-256 hash of: "$salt:$flagKey:${stableId.hexId}"
// Reduced to range [0, 9999] for 0.01% precision
// Bucket < threshold → user is in rollout

val bucket = hash("v1:darkMode:757365722d313233") % 10000
val threshold = (50.0 * 100).toInt()  // 5000 for 50%
val inRollout = bucket < threshold
```

**Guarantees**:
- Same `(stableId, flagKey, salt)` → same bucket (deterministic)
- Increasing percentage (10% → 20%) only adds users
- Different flags → independent buckets (`flagKey` in hash)
- Changing salt → redistributes buckets (intentional reshuffle)

### Evaluation with Reason
```kotlin
val result = Features.darkMode.evaluateWithReason(context)
println(result.decision)  // RuleMatched | DefaultReturned | Disabled
println(result.matchedRule?.constraints)
println(result.bucketInfo)
```

## Examples

### Platform Override (Specificity Wins)
```kotlin
val feature by boolean(default = false) {
    rule(true) { platforms(Platform.IOS) }              // specificity: 1
    rule(true) { rollout { 50.0 } }                     // specificity: 0
}

// Context: iOS, bucket = 6000 (60%, outside 50% rollout)
// Result: true (first rule matched by platform, rollout not checked)
```

**Explanation**: Platform rule has higher specificity, so it's evaluated first. It matches iOS regardless of bucket.

### Rollout Gating
```kotlin
val feature by boolean(default = false) {
    rule(true) {
        platforms(Platform.ANDROID)
        rollout { 30.0 }
    }
}

// Context: Android, bucket = 2500 (25%, inside 30%)
// Result: true (criteria matched AND in rollout)

// Context: Android, bucket = 4000 (40%, outside 30%)
// Result: false (criteria matched BUT outside rollout → default)
```

### Allowlist Bypass
```kotlin
val feature by boolean(default = false) {
    allowlist(StableId.of("tester-1"))
    rule(true) { rollout { 5.0 } }
}

// Context: stableId = "tester-1", bucket = 8000 (80%, outside 5%)
// Result: true (allowlisted users bypass rollout check)

// Context: stableId = "user-123", bucket = 8000 (80%, outside 5%)
// Result: false (not allowlisted, outside rollout → default)
```

### Multiple Rules (Specificity Order)
```kotlin
val feature by boolean(default = false) {
    rule(true) { platforms(Platform.IOS); locales(AppLocale.UNITED_STATES) }  // 2
    rule(true) { platforms(Platform.IOS) }                                     // 1
    rule(true) { rollout { 50.0 } }                                            // 0
}

// Context: iOS + France
// Evaluation:
// - Rule 1 (specificity 2): iOS✓ France✗ → skip
// - Rule 2 (specificity 1): iOS✓ → return true
// - Rule 3 never evaluated
```

### Common Misconceptions

**Misconception 1: Rollout affects precedence**
**Wrong**: "Higher rollout percentage means higher priority"
**Right**: "Rollout only gates application after specificity determines order"

**Misconception 2: Last rule wins**
**Wrong**: "The last rule I define will be checked first"
**Right**: "Rules are sorted by specificity, not definition order"

**Misconception 3: Partial match applies**
**Wrong**: "If some criteria match, the rule partially applies"
**Right**: "ALL criteria must match for a rule to be considered (AND semantics)"

**Misconception 4: Rollout is random**
**Wrong**: "Each evaluation randomly assigns users to rollout"
**Right**: "Bucketing is deterministic—same inputs always produce same bucket"
