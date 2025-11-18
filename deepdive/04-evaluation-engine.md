# Engineering Deep Dive: Evaluation Engine

**Navigate**: [← Previous: Type System](03-type-system.md) | [Next: Rules & Specificity →](05-rules-specificity.md)

---

## The Heart of Feature Flag Evaluation

The evaluation engine is where all the pieces come together: Features, Contexts, Rules, and Values. This chapter dissects the `FlagDefinition.evaluate()` method and reveals how Konditional determines which value to return for a given context.

## FlagDefinition: The Central Actor

Every feature flag has a `FlagDefinition` that holds its configuration:

```kotlin
data class FlagDefinition<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>(
    val defaultValue: T,
    val feature: Feature<S, T, C, M>,
    val values: List<ConditionalValue<S, T, C, M>>,
    val isActive: Boolean = true,
    val salt: String = "v1"
)
```

Let's understand each component:

### defaultValue: T

The fallback value returned when:
- No rules match the context
- The flag is inactive
- All matching rules fail rollout bucketing

**Why required**: Eliminates null handling. Every evaluation always returns a value.

### feature: Feature<S, T, C, M>

The feature identifier this definition implements. Provides:
- `key`: String identifier for serialization and registry lookup
- `namespace`: Isolation boundary for this flag

### values: List<ConditionalValue<S, T, C, M>>

The targeting rules. Each `ConditionalValue` pairs a `Rule` with a target value:

```kotlin
data class ConditionalValue<S, T, C, M>(
    val rule: Rule<C>,
    val value: T
)
```

When a rule matches, its paired value is returned.

### isActive: Boolean

Kill switch for the entire flag. When `false`:
- Evaluation short-circuits immediately
- `defaultValue` is always returned
- No rule matching or bucketing occurs

**Use case**: Emergency disable without configuration redeployment.

### salt: String

Version identifier for bucketing. Changing the salt redistributes users across rollout buckets.

**Use case**: Re-randomize rollout assignments when needed (e.g., after detecting bias).

---

## The Evaluation Algorithm

Here's the complete implementation from `FlagDefinition.kt`:

```kotlin
fun evaluate(context: C): T {
    if (!isActive) return defaultValue

    return conditionalValues.firstOrNull {
        it.rule.matches(context) &&
            isInEligibleSegment(
                flagKey = feature.key,
                id = context.stableId.hexId,
                salt = salt,
                rollout = it.rule.rollout
            )
    }?.value ?: defaultValue
}
```

Let's break this down step by step.

### Step 1: Inactive Check

```kotlin
if (!isActive) return defaultValue
```

**What**: First operation in evaluation
**When**: Flag has been marked inactive (kill switch)
**Result**: Return `defaultValue` immediately, skip all other logic

**Why this order**: Performance. Inactive flags bypass all rule matching and bucketing computation.

**Example**:
```kotlin
val definition = FlagDefinition(
    feature = DARK_MODE,
    defaultValue = false,
    values = listOf(/* 100 complex rules */),
    isActive = false  // Kill switch activated
)

val result = definition.evaluate(context)
// → false (immediately, no rule evaluation)
```

### Step 2: Rule Iteration

```kotlin
return conditionalValues.firstOrNull { /* ... */ }?.value ?: defaultValue
```

**What**: Iterate through rules in specificity order
**When**: Flag is active
**Result**: First matching rule's value, or `defaultValue` if none match

**Key insight**: `conditionalValues` is pre-sorted by specificity (we'll see how in a moment).

### Step 3: Rule Matching

```kotlin
it.rule.matches(context)
```

**What**: Check if the rule's conditions match the context
**When**: For each rule in specificity order
**Result**: `true` if all conditions match, `false` otherwise

**Rule matching checks**:
- Locale constraints (if specified)
- Platform constraints (if specified)
- Version range constraints (if specified)
- Extension conditions (if provided)

We'll explore matching in detail in Chapter 5.

### Step 4: Rollout Bucketing

```kotlin
isInEligibleSegment(
    flagKey = feature.key,
    id = context.stableId.hexId,
    salt = salt,
    rollout = it.rule.rollout
)
```

**What**: Deterministically check if this user is in the rollout bucket
**When**: After rule matches context
**Result**: `true` if user is in rollout, `false` otherwise

**Why after matching**: No need to compute expensive hash if rule doesn't match.

We'll explore bucketing in detail in Chapter 6.

### Step 5: Value Extraction

```kotlin
.value ?: defaultValue
```

**What**: Extract the value from the matching rule, or use default
**When**: After finding a matching rule (or finding none)
**Result**: The target value of type `T`

**Type safety**: Compiler guarantees `value` is type `T` and never null.

---

## Rule Sorting: Specificity Order

Before evaluation begins, rules are sorted by specificity:

```kotlin
private val conditionalValues: List<ConditionalValue<S, T, C, M>> =
    values.sortedWith(
        compareByDescending<ConditionalValue<S, T, C, M>> { it.rule.specificity() }
            .thenBy { it.rule.note ?: "" }
    )
```

### Why Sort?

**Problem**: Multiple rules might match the same context.

**Question**: Which one wins?

**Answer**: The most specific one.

### The Sorting Strategy

```kotlin
compareByDescending { it.rule.specificity() }
```

**Primary sort**: Higher specificity first
- Rule with specificity 3 evaluated before rule with specificity 1
- More specific rules have priority

```kotlin
.thenBy { it.rule.note ?: "" }
```

**Secondary sort**: Alphabetical by note
- If two rules have same specificity, sort by note
- Provides deterministic ordering
- Prevents non-deterministic behavior

### Specificity Example

```kotlin
val FEATURE by boolean(default = false) {
    // Rule 1: Specificity = 1 (only platform)
    rule {
        platforms(Platform.IOS)
    } returns true

    // Rule 2: Specificity = 2 (platform + version)
    rule {
        platforms(Platform.IOS)
        versions { min(2, 0, 0) }
    } returns false

    // Rule 3: Specificity = 3 (platform + version + locale)
    rule {
        platforms(Platform.IOS)
        versions { min(2, 0, 0) }
        locales(AppLocale.EN_US)
    } returns true
}
```

**Evaluation order**: Rule 3 → Rule 2 → Rule 1 → Default

**For context** `(IOS, 2.1.0, EN_US)`:
1. Rule 3 matches (all three constraints) → Returns `true`
2. Rules 2 and 1 never evaluated (first match wins)

**For context** `(IOS, 2.1.0, FR_FR)`:
1. Rule 3 doesn't match (locale mismatch)
2. Rule 2 matches (platform + version) → Returns `false`
3. Rule 1 never evaluated

**For context** `(IOS, 1.5.0, EN_US)`:
1. Rule 3 doesn't match (version too low)
2. Rule 2 doesn't match (version too low)
3. Rule 1 matches (platform only) → Returns `true`

### Why Most Specific First?

**Principle**: Specific rules override general rules.

Think of it like CSS specificity:
```css
/* General rule */
button { color: blue; }

/* More specific rule */
button.primary { color: green; }

/* Most specific rule */
button.primary.large { color: red; }
```

The most specific selector wins. Same principle here.

---

## Evaluation Flow Diagram

```
Context.evaluate(Feature)
         ↓
    Lookup FlagDefinition in Namespace Registry
         ↓
    FlagDefinition.evaluate(Context)
         ↓
    ┌──────────────────┐
    │  Is Active?      │───No──→ Return defaultValue
    └──────────────────┘
         │ Yes
         ↓
    ┌──────────────────┐
    │ Iterate Rules    │ (in specificity order)
    │ (sorted)         │
    └──────────────────┘
         ↓
    ┌──────────────────┐
    │ Rule Matches     │───No──→ Try next rule
    │ Context?         │
    └──────────────────┘
         │ Yes
         ↓
    ┌──────────────────┐
    │ In Rollout       │───No──→ Try next rule
    │ Bucket?          │
    └──────────────────┘
         │ Yes
         ↓
    Return rule.value
         ↓
    (If no rules match)
         ↓
    Return defaultValue
```

---

## ConditionalValue: Pairing Rules with Values

A `ConditionalValue` is simply a rule paired with its target value:

```kotlin
data class ConditionalValue<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>(
    val rule: Rule<C>,
    val value: T
)
```

### Creation Syntax

In the DSL:
```kotlin
rule {
    platforms(Platform.IOS)
} returns true
// ^^^^^^^^^^^
// Creates ConditionalValue(rule, true)
```

Internal implementation:
```kotlin
infix fun Rule<C>.returns(value: T): ConditionalValue<S, T, C, M> =
    ConditionalValue(this, value)
```

### Why Separate?

**Alternative design**: Rules could hold their target values directly.

**Why not**:
```kotlin
// Hypothetical: Rule holding value
data class Rule<T>(
    val conditions: ...,
    val value: T  // ← Type parameter added
)
```

**Problems**:
1. Rules become value-type-specific
2. Can't reuse rules across different flag types
3. Complicates rule composition

**Current design benefits**:
```kotlin
// Rule is value-agnostic
val iosRule: Rule<Context> = Rule(platforms = setOf(Platform.IOS))

// Can pair with any value type
val boolFlag: ConditionalValue<_, Boolean, _, _> = ConditionalValue(iosRule, true)
val stringFlag: ConditionalValue<_, String, _, _> = ConditionalValue(iosRule, "iOS")
val intFlag: ConditionalValue<_, Int, _, _> = ConditionalValue(iosRule, 42)
```

Rule reusability maintained.

---

## Complete Evaluation Example

Let's trace a complete evaluation:

### Setup

```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val NEW_CHECKOUT by boolean(default = false) {
        rule {
            note = "US iOS users on v2+"
            platforms(Platform.IOS)
            versions { min(2, 0, 0) }
            locales(AppLocale.EN_US)
            rollout { 50.0 }
        } returns true

        rule {
            note = "All iOS users"
            platforms(Platform.IOS)
            rollout { 10.0 }
        } returns true
    }
}
```

### Context

```kotlin
val context = Context(
    locale = AppLocale.EN_US,
    platform = Platform.IOS,
    appVersion = Version(2, 1, 0),
    stableId = StableId.of("user123abc...")  // Hypothetical stable ID
)
```

### Evaluation Trace

```kotlin
definition.evaluate(context)

// Step 1: Active check
isActive = true  // ✓ Continue

// Step 2: Sorted rules (by specificity)
// Rule 1: specificity = 3 (platform + version + locale)
// Rule 2: specificity = 1 (platform only)

// Step 3: Try Rule 1
rule1.matches(context)
  → platforms: IOS in {IOS} ✓
  → versions: 2.1.0 >= 2.0.0 ✓
  → locales: EN_US in {EN_US} ✓
  → Result: true ✓

// Step 4: Check rollout for Rule 1
isInEligibleSegment(
    flagKey = "NEW_CHECKOUT",
    id = "user123abc...",
    salt = "v1",
    rollout = 50.0
)
  → bucket = stableBucket(...) = 3456  // Hypothetical
  → threshold = 50.0 * 100 = 5000
  → 3456 < 5000 ✓
  → Result: true ✓

// Step 5: Return Rule 1's value
return true
```

**Result**: `true`

### Alternative Scenario

Same context, but user's bucket is 7890 (outside 50% rollout):

```kotlin
// Steps 1-3: Same (rule matches)

// Step 4: Check rollout for Rule 1
isInEligibleSegment(...)
  → bucket = 7890
  → threshold = 5000
  → 7890 < 5000 ✗
  → Result: false ✗

// Rule 1 fails rollout, try Rule 2

// Step 3: Try Rule 2
rule2.matches(context)
  → platforms: IOS in {IOS} ✓
  → Result: true ✓

// Step 4: Check rollout for Rule 2
isInEligibleSegment(
    rollout = 10.0
)
  → bucket = 7890
  → threshold = 1000
  → 7890 < 1000 ✗
  → Result: false ✗

// Rule 2 fails rollout, no more rules

// Step 5: Return default
return false
```

**Result**: `false`

---

## Why This Design?

### 1. Fail-Safe Defaults

Every flag always returns a value. No null checks, no exceptions.

```kotlin
// Always returns Boolean, never null
val enabled: Boolean = context.evaluate(FEATURE)
```

### 2. Explicit Prioritization

Most specific rules win automatically. No manual ordering needed.

```kotlin
// Automatically sorted by specificity
// Developer just declares rules, system orders them
```

### 3. Efficient Short-Circuiting

```kotlin
// Fast path for inactive flags
if (!isActive) return defaultValue

// Fast path for matching
conditionalValues.firstOrNull { /* ... */ }
// Stops at first match, doesn't evaluate remaining rules
```

### 4. Deterministic Evaluation

- Same context always produces same result
- Rollout bucketing is deterministic (SHA-256 based)
- Rule ordering is deterministic (specificity + note)

No race conditions, no non-determinism.

### 5. Composable Rules

Rules are independent of value types. Can reuse rule logic across different flags.

---

## Performance Characteristics

### Time Complexity

**Best case**: O(1)
- Flag is inactive: immediate return
- First rule matches: one iteration

**Worst case**: O(n)
- All rules evaluated
- n = number of rules
- In practice, n is small (typically < 10 rules per flag)

**Average case**: O(k) where k < n
- Rules sorted by specificity
- Most specific rules tried first
- Common contexts match early

### Space Complexity

**Per flag**: O(n)
- n = number of rules
- Each rule stores conditions and value

**Sorted list**: O(n)
- Pre-computed at flag creation
- No runtime sorting overhead

### Optimizations

**Pre-sorting**:
```kotlin
private val conditionalValues = values.sortedWith(...)
```
- Sorted once at construction
- Evaluation uses sorted list
- No per-evaluation sort cost

**Short-circuit matching**:
```kotlin
conditionalValues.firstOrNull { /* ... */ }
```
- Stops at first match
- Doesn't evaluate remaining rules
- Lazy evaluation

**Lazy bucketing**:
```kotlin
it.rule.matches(context) && isInEligibleSegment(...)
```
- Boolean AND short-circuits
- If `matches()` returns false, bucketing not computed
- Expensive SHA-256 avoided when possible

---

## Edge Cases and Gotchas

### Edge Case 1: No Rules

```kotlin
val FEATURE by boolean(default = false) {
    // No rules defined
}

context.evaluate(FEATURE)  // → false (default)
```

Perfectly valid. Flag always returns default.

### Edge Case 2: All Rules Fail

```kotlin
val FEATURE by boolean(default = false) {
    rule { platforms(Platform.ANDROID) } returns true
}

// iOS context
val context = Context(platform = Platform.IOS, ...)
context.evaluate(FEATURE)  // → false (default)
```

No Android match, returns default.

### Edge Case 3: Multiple Rules Same Specificity

```kotlin
val FEATURE by boolean(default = false) {
    rule {
        note = "Rule A"
        platforms(Platform.IOS)
    } returns true

    rule {
        note = "Rule B"
        platforms(Platform.ANDROID)
    } returns false
}
```

Both have specificity = 1. Sorted by note:
- "Rule A" before "Rule B" (alphabetical)
- But they target different platforms, so no overlap
- Order doesn't matter in practice

**Best practice**: If rules might overlap, give them different specificity or unique notes for deterministic ordering.

### Edge Case 4: Inactive with 100% Rollout

```kotlin
val definition = FlagDefinition(
    isActive = false,
    values = listOf(
        ConditionalValue(
            Rule(rollout = Rollout.of(100.0)),
            value = true
        )
    ),
    defaultValue = false
)

definition.evaluate(context)  // → false (inactive wins)
```

Inactive check happens first. Even 100% rollout is ignored.

---

## Review: The Evaluation Flow

1. **Inactive check**: Short-circuit if flag is off
2. **Rule iteration**: Try rules in specificity order (highest first)
3. **Rule matching**: Check if context satisfies rule conditions
4. **Rollout bucketing**: Deterministic hash-based inclusion check
5. **Value extraction**: Return matched value or default

**Key properties**:
- Always returns a value (never null)
- Deterministic (same input → same output)
- Efficient (short-circuits when possible)
- Type-safe (compiler enforces value types)

---

## Next Steps

Now that you understand how evaluation works, we can explore how specificity is calculated and why it matters.

**Next chapter**: [Rules & Specificity](05-rules-specificity.md)
- BaseEvaluable implementation details
- Specificity calculation algorithm
- Why more specific rules win
- Extension evaluables and custom specificity

Specificity is what makes automatic rule prioritization possible. Let's see how it works.

---

**Navigate**: [← Previous: Type System](03-type-system.md) | [Next: Rules & Specificity →](05-rules-specificity.md)
