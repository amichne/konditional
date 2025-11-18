# Engineering Deep Dive: Rules & Specificity

**Navigate**: [← Previous: Evaluation Engine](04-evaluation-engine.md) | [Next: Bucketing Algorithm →](06-bucketing-algorithm.md)

---

## Automatic Rule Prioritization

In Chapter 4, we saw that rules are evaluated in specificity order. This chapter reveals how specificity is calculated and why it ensures the "right" rule always wins.

## The Specificity Problem

Consider this scenario:

```kotlin
val FEATURE by boolean(default = false) {
    // General rule: All iOS users
    rule {
        platforms(Platform.IOS)
    } returns true

    // Specific rule: iOS users in US
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.EN_US)
    } returns false
}
```

**Question**: Which rule should win for a US iOS user?

**Answer**: The more specific one (iOS + US locale).

**Challenge**: How does the system know which is more specific?

**Solution**: Automatic specificity calculation.

---

## The Evaluable Interface

Both `Rule` and `BaseEvaluable` implement the `Evaluable` interface:

```kotlin
interface Evaluable<C : Context> {
    /**
     * Determines if the context matches this evaluable's conditions.
     */
    fun matches(context: C): Boolean

    /**
     * Calculates the specificity of this evaluable.
     * Higher values indicate more specific targeting.
     */
    fun specificity(): Int
}
```

Two responsibilities:
1. **Matching**: Does this context satisfy the conditions?
2. **Specificity**: How specific are these conditions?

---

## BaseEvaluable: Standard Targeting

`BaseEvaluable` handles standard targeting dimensions:

```kotlin
internal data class BaseEvaluable<C : Context>(
    val locales: Set<AppLocale> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val versionRange: VersionRange = Unbounded(),
) : Evaluable<C>
```

### The Three Dimensions

#### 1. Locales

```kotlin
val locales: Set<AppLocale> = emptySet()
```

**Empty = match all**: If `locales` is empty, any locale matches.
**Non-empty = constrained**: Context locale must be in the set.

**Example**:
```kotlin
BaseEvaluable(locales = setOf(AppLocale.EN_US, AppLocale.EN_CA))

// Matches:
context.locale = EN_US  ✓
context.locale = EN_CA  ✓

// Doesn't match:
context.locale = FR_FR  ✗
```

#### 2. Platforms

```kotlin
val platforms: Set<Platform> = emptySet()
```

**Empty = match all**: If `platforms` is empty, any platform matches.
**Non-empty = constrained**: Context platform must be in the set.

**Example**:
```kotlin
BaseEvaluable(platforms = setOf(Platform.IOS, Platform.ANDROID))

// Matches:
context.platform = IOS      ✓
context.platform = ANDROID  ✓

// Doesn't match:
context.platform = WEB  ✗
```

#### 3. Version Range

```kotlin
val versionRange: VersionRange = Unbounded()
```

**Unbounded = match all**: If `versionRange` is `Unbounded`, any version matches.
**Bounded = constrained**: Context version must be in range.

**Example**:
```kotlin
BaseEvaluable(versionRange = LeftBound(Version(2, 0, 0)))  // >= 2.0.0

// Matches:
context.appVersion = Version(2, 0, 0)  ✓
context.appVersion = Version(2, 1, 5)  ✓
context.appVersion = Version(3, 0, 0)  ✓

// Doesn't match:
context.appVersion = Version(1, 9, 9)  ✗
```

### The Matching Algorithm

```kotlin
override fun matches(context: C): Boolean =
    (locales.isEmpty() || context.locale in locales) &&
        (platforms.isEmpty() || context.platform in platforms) &&
        (!versionRange.hasBounds() || versionRange.contains(context.appVersion))
```

**Logic**:
- **Locales**: Empty OR context locale in set
- **Platforms**: Empty OR context platform in set
- **Version**: Unbounded OR context version in range
- **Result**: ALL conditions must be true

**Key insight**: Empty constraints are permissive (match all). Non-empty constraints are restrictive (must match).

### The Specificity Algorithm

```kotlin
override fun specificity(): Int =
    (if (locales.isNotEmpty()) 1 else 0) +
    (if (platforms.isNotEmpty()) 1 else 0) +
    (if (versionRange.hasBounds()) 1 else 0)
```

**Simple counting**: Each non-empty constraint adds 1 to specificity.

**Range**: 0 (no constraints) to 3 (all constraints)

**Examples**:

```kotlin
// Specificity = 0 (no constraints, matches everything)
BaseEvaluable()

// Specificity = 1 (one constraint)
BaseEvaluable(platforms = setOf(Platform.IOS))
BaseEvaluable(locales = setOf(AppLocale.EN_US))
BaseEvaluable(versionRange = LeftBound(Version(2, 0, 0)))

// Specificity = 2 (two constraints)
BaseEvaluable(
    platforms = setOf(Platform.IOS),
    locales = setOf(AppLocale.EN_US)
)

// Specificity = 3 (all constraints)
BaseEvaluable(
    platforms = setOf(Platform.IOS),
    locales = setOf(AppLocale.EN_US),
    versionRange = LeftBound(Version(2, 0, 0))
)
```

---

## Rule: Composable Evaluation

`Rule` composes `BaseEvaluable` with optional extension logic:

```kotlin
data class Rule<C : Context>(
    val rollout: Rollout = Rollout.default,
    val note: String? = null,
    internal val baseEvaluable: BaseEvaluable<C> = BaseEvaluable(),
    val extension: Evaluable<C> = Placeholder,
) : Evaluable<C>
```

### Component Breakdown

#### rollout: Rollout

Percentage of users who should match (after other conditions are met).

**Not part of matching**: Rollout is checked separately in evaluation engine.
**Range**: 0.0 to 100.0

#### note: String?

Optional description for debugging and deterministic sorting.

**Used in sort**: When two rules have same specificity, sorted alphabetically by note.

#### baseEvaluable: BaseEvaluable<C>

Standard targeting (locale, platform, version).

#### extension: Evaluable<C>

Custom evaluation logic. Defaults to `Placeholder` (always matches, specificity 0).

### Composed Matching

```kotlin
override fun matches(context: C): Boolean =
    baseEvaluable.matches(context) && extension.matches(context)
```

**Both must match**: Rule matches only if both base AND extension match.

**Short-circuit**: If `baseEvaluable` doesn't match, `extension` not evaluated.

### Composed Specificity

```kotlin
override fun specificity(): Int =
    baseEvaluable.specificity() + extension.specificity()
```

**Additive**: Total specificity is the sum of both components.

**Examples**:

```kotlin
// Base specificity = 2, Extension specificity = 0
// Total = 2
Rule(
    baseEvaluable = BaseEvaluable(
        platforms = setOf(Platform.IOS),
        locales = setOf(AppLocale.EN_US)
    ),
    extension = Placeholder  // Specificity 0
)

// Base specificity = 1, Extension specificity = 1
// Total = 2
Rule(
    baseEvaluable = BaseEvaluable(
        platforms = setOf(Platform.IOS)
    ),
    extension = object : Evaluable<MyContext> {
        override fun matches(context: MyContext) = context.isPremiumUser
        override fun specificity() = 1
    }
)
```

---

## Why Specificity Works

### Principle: Specificity Reflects Constraint Count

**More constraints = more specific = higher priority**

This mirrors intuition:
- "iOS users" is general
- "iOS users in US" is more specific
- "iOS users in US on version 2+" is most specific

### The Counting Heuristic

Counting constraints works because:

1. **Empty constraints are universal**: They match everything
2. **Non-empty constraints are restrictive**: They exclude contexts
3. **More restrictions = narrower target = more specific**

### Mathematical View

Think of each constraint as filtering a set:

```
All Contexts (universal set)
    ↓ Filter by platform = IOS
IOS Contexts (subset)
    ↓ Filter by locale = EN_US
IOS + EN_US Contexts (smaller subset)
    ↓ Filter by version >= 2.0.0
IOS + EN_US + v2+ Contexts (smallest subset)
```

**Smaller target set = more specific rule**

Specificity = number of filters applied.

---

## Specificity Examples

### Example 1: Platform Targeting

```kotlin
val FEATURE by boolean(default = false) {
    // Rule 1: Specificity = 1 (platform only)
    rule {
        platforms(Platform.IOS, Platform.ANDROID)
    } returns true

    // Rule 2: Specificity = 0 (no constraints)
    rule {
        // Empty - matches all
    } returns false
}
```

**Evaluation order**: Rule 1 → Rule 2

**For any iOS/Android context**: Rule 1 matches → `true`
**For web context**: Rule 1 doesn't match, Rule 2 matches → `false`

### Example 2: Layered Rollout

```kotlin
val NEW_UI by boolean(default = false) {
    // Rule 1: Specificity = 3 (all constraints)
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.EN_US)
        versions { min(2, 0, 0) }
        rollout { 100.0 }  // 100% of this specific segment
    } returns true

    // Rule 2: Specificity = 2 (platform + version)
    rule {
        platforms(Platform.IOS)
        versions { min(2, 0, 0) }
        rollout { 50.0 }  // 50% of broader segment
    } returns true

    // Rule 3: Specificity = 1 (platform only)
    rule {
        platforms(Platform.IOS)
        rollout { 10.0 }  // 10% of broadest segment
    } returns true
}
```

**Evaluation order**: Rule 1 → Rule 2 → Rule 3 → Default

**For US iOS v2+ user**:
1. Rule 1 matches → 100% rollout → `true`

**For FR iOS v2+ user**:
1. Rule 1 doesn't match (locale)
2. Rule 2 matches → 50% rollout → maybe `true`

**For US iOS v1.5 user**:
1. Rule 1 doesn't match (version)
2. Rule 2 doesn't match (version)
3. Rule 3 matches → 10% rollout → maybe `true`

**Insight**: More specific segments get higher rollout percentages. This is a common pattern.

### Example 3: Extension Specificity

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: SubscriptionTier
) : Context

val FEATURE by boolean<EnterpriseContext>(default = false) {
    // Rule 1: Specificity = 2 (platform + extension)
    rule {
        platforms(Platform.IOS)
        extension {
            Evaluable.factory { ctx: EnterpriseContext ->
                ctx.subscriptionTier == SubscriptionTier.ENTERPRISE
            }.withSpecificity(1)
        }
    } returns true

    // Rule 2: Specificity = 1 (platform only)
    rule {
        platforms(Platform.IOS)
    } returns false
}
```

**Evaluation order**: Rule 1 → Rule 2

**For enterprise iOS user**: Rule 1 matches → `true`
**For non-enterprise iOS user**: Rule 1 doesn't match (extension fails), Rule 2 matches → `false`

---

## Extension Evaluables

Custom logic for domain-specific conditions.

### The Placeholder

Default extension that always matches with zero specificity:

```kotlin
object Placeholder : Evaluable<Context> {
    override fun matches(context: Context): Boolean = true
    override fun specificity(): Int = 0
}
```

**Used when**: No custom logic needed.

### Custom Evaluables

Implement `Evaluable` for custom conditions:

```kotlin
class PremiumUserEvaluable : Evaluable<EnterpriseContext> {
    override fun matches(context: EnterpriseContext): Boolean =
        context.subscriptionTier == SubscriptionTier.ENTERPRISE

    override fun specificity(): Int = 1
}

// Use in rule:
rule {
    platforms(Platform.IOS)
    extension(PremiumUserEvaluable())
} returns true
```

### Specificity Guidelines for Extensions

**Specificity 0**: Universal conditions (always true, or very broad)
- "User is logged in"
- "Request is authenticated"

**Specificity 1**: Single dimension
- "User is premium subscriber"
- "Organization has feature flag enabled"

**Specificity 2**: Two dimensions
- "User is premium AND organization is enterprise"
- "User is in beta group AND has opted into experiments"

**Specificity 3+**: Multiple dimensions
- Domain-specific complex conditions

**Principle**: Specificity should reflect how restrictive the condition is.

---

## Rule Sorting Algorithm

Recall from Chapter 4:

```kotlin
private val conditionalValues: List<ConditionalValue<S, T, C, M>> =
    values.sortedWith(
        compareByDescending<ConditionalValue<S, T, C, M>> { it.rule.specificity() }
            .thenBy { it.rule.note ?: "" }
    )
```

### Primary Sort: Specificity (Descending)

```kotlin
compareByDescending { it.rule.specificity() }
```

**Higher specificity first**: Rules with more constraints evaluated before rules with fewer constraints.

### Secondary Sort: Note (Ascending)

```kotlin
.thenBy { it.rule.note ?: "" }
```

**Alphabetical**: When specificity is equal, sort by note alphabetically.

**Why**: Ensures deterministic ordering when multiple rules have same specificity.

### Example Sorting

```kotlin
val rules = listOf(
    Rule(note = "B", baseEvaluable = BaseEvaluable(platforms = setOf(Platform.IOS))),  // Specificity 1
    Rule(note = "A", baseEvaluable = BaseEvaluable(
        platforms = setOf(Platform.IOS),
        locales = setOf(AppLocale.EN_US)
    )),  // Specificity 2
    Rule(note = "C", baseEvaluable = BaseEvaluable()),  // Specificity 0
    Rule(note = "D", baseEvaluable = BaseEvaluable(platforms = setOf(Platform.ANDROID))),  // Specificity 1
)

// After sorting:
// 1. "A" (specificity 2)
// 2. "B" (specificity 1, note "B" < "D")
// 3. "D" (specificity 1, note "D" > "B")
// 4. "C" (specificity 0)
```

---

## Why More Specific Rules Win

### Design Rationale

**User expectation**: Specific rules override general rules.

**Example from configuration**:
```yaml
# General config
timeout: 30

# Specific override for production
production:
  timeout: 60
```

Users expect production override to win. Same principle here.

### Alternative: Manual Ordering

**Without specificity**:
```kotlin
val FEATURE by boolean(default = false) {
    rule(priority = 100) { /* specific */ } returns true
    rule(priority = 50) { /* general */ } returns false
}
```

**Problems**:
1. Developer must manually assign priorities
2. Easy to make mistakes (forget to update priorities)
3. Non-obvious what priority values mean
4. No automatic checking of correctness

**With specificity**:
```kotlin
val FEATURE by boolean(default = false) {
    rule { /* specific constraints */ } returns true  // Auto-prioritized
    rule { /* general constraints */ } returns false  // Auto-prioritized
}
```

**Benefits**:
1. Automatic prioritization
2. Obvious from constraints alone
3. Impossible to mess up ordering
4. Follows principle of least surprise

---

## Edge Cases

### Edge Case 1: All Rules Same Specificity

```kotlin
val FEATURE by boolean(default = false) {
    rule {
        note = "iOS"
        platforms(Platform.IOS)
    } returns true

    rule {
        note = "Android"
        platforms(Platform.ANDROID)
    } returns false
}
```

**Both specificity = 1**

**Sorted by note**: "Android" before "iOS" (alphabetically)

**But**: They target different platforms, so no overlap. Order doesn't matter.

**Best practice**: If rules might overlap, ensure different specificity or unique notes for deterministic ordering.

### Edge Case 2: Extension Overtakes Base

```kotlin
val FEATURE by boolean<EnterpriseContext>(default = false) {
    // Rule 1: Base = 1, Extension = 2, Total = 3
    rule {
        platforms(Platform.IOS)
        extension {
            ComplexBusinessLogic().withSpecificity(2)
        }
    } returns true

    // Rule 2: Base = 2, Extension = 0, Total = 2
    rule {
        platforms(Platform.IOS)
        locales(AppLocale.EN_US)
    } returns false
}
```

**Evaluation order**: Rule 1 (specificity 3) → Rule 2 (specificity 2)

**Insight**: Extension specificity can make a rule with fewer base constraints more specific overall.

**When to use**: When custom business logic is more important than standard targeting.

### Edge Case 3: Zero Specificity Rule

```kotlin
val FEATURE by boolean(default = false) {
    rule {
        // No constraints
        rollout { 50.0 }
    } returns true
}
```

**Specificity = 0** (no constraints)

**Matches**: All contexts (then 50% rollout applies)

**Equivalent to**: 50% rollout on the default value

**Common pattern**: Gradual rollout to everyone without targeting.

---

## Specificity in Practice

### Pattern 1: Geographic Rollout

```kotlin
val FEATURE by boolean(default = false) {
    // Stage 1: US only, 100% rollout
    rule {
        locales(AppLocale.EN_US)
        rollout { 100.0 }
    } returns true

    // Stage 2: English-speaking countries, 50% rollout
    rule {
        locales(AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB)
        rollout { 50.0 }
    } returns true

    // Stage 3: All countries, 10% rollout
    rule {
        rollout { 10.0 }
    } returns true
}
```

**Wait, problem!** All rules have same base specificity (1 or 0).

**Solution**: Use note for deterministic ordering:

```kotlin
val FEATURE by boolean(default = false) {
    rule {
        note = "1-US-100%"
        locales(AppLocale.EN_US)
        rollout { 100.0 }
    } returns true

    rule {
        note = "2-English-50%"
        locales(AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB)
        rollout { 50.0 }
    } returns true

    rule {
        note = "3-All-10%"
        rollout { 10.0 }
    } returns true
}
```

**Better solution**: Add more constraints to US rule:

```kotlin
val FEATURE by boolean(default = false) {
    // Specificity = 2
    rule {
        locales(AppLocale.EN_US)
        platforms(Platform.IOS, Platform.ANDROID)  // Additional constraint
        rollout { 100.0 }
    } returns true

    // Specificity = 1
    rule {
        locales(AppLocale.EN_US, AppLocale.EN_CA, AppLocale.EN_GB)
        rollout { 50.0 }
    } returns true

    // Specificity = 0
    rule {
        rollout { 10.0 }
    } returns true
}
```

### Pattern 2: Beta Testing

```kotlin
val BETA_FEATURE by boolean<EnterpriseContext>(default = false) {
    // Specificity = 2 (platform + extension)
    rule {
        platforms(Platform.IOS)
        extension {
            BetaUserEvaluable().withSpecificity(1)
        }
        rollout { 100.0 }
    } returns true

    // Specificity = 1 (platform only)
    rule {
        platforms(Platform.IOS)
        rollout { 10.0 }  // Gradual rollout to non-beta users
    } returns true
}
```

**Beta users**: Always enabled (100% rollout)
**Non-beta users**: 10% rollout

### Pattern 3: Emergency Rollback

```kotlin
val NEW_PAYMENT_FLOW by boolean(default = false) {
    // Specificity = 2 (platform + version)
    // Rolled out, but bug found in older versions
    rule {
        platforms(Platform.IOS)
        versions { min(2, 1, 0) }  // Only v2.1.0+, bug exists in v2.0.x
        rollout { 100.0 }
    } returns true

    // Specificity = 1 (platform only)
    // Fallback for v2.0.x users
    rule {
        platforms(Platform.IOS)
        rollout { 0.0 }  // Disabled due to bug
    } returns false
}
```

**v2.1.0+ users**: New flow enabled
**v2.0.x users**: Old flow (bug workaround)

---

## Review: Specificity System

### Core Concept

**Specificity = number of constraints**

More constraints = more specific = higher priority.

### BaseEvaluable Specificity

```kotlin
specificity =
    (if (locales.isNotEmpty()) 1 else 0) +
    (if (platforms.isNotEmpty()) 1 else 0) +
    (if (versionRange.hasBounds()) 1 else 0)
```

Range: 0 to 3

### Rule Specificity

```kotlin
specificity = baseEvaluable.specificity() + extension.specificity()
```

Additive composition.

### Sorting

```kotlin
compareByDescending { it.rule.specificity() }
    .thenBy { it.rule.note ?: "" }
```

Primary: specificity (descending)
Secondary: note (alphabetical)

### Why It Works

- Reflects constraint count
- Mirrors user intuition (specific overrides general)
- Automatic (no manual priorities)
- Deterministic (same rules → same order)

---

## Next Steps

Now that you understand how rules are prioritized, we can explore the bucketing algorithm that determines rollout inclusion.

**Next chapter**: [Bucketing Algorithm](06-bucketing-algorithm.md)
- SHA-256 hashing implementation
- The stableBucket() function
- Why SHA-256 over simpler hashing
- Rollout threshold comparison
- Salt's role in redistribution

Bucketing is what makes gradual rollouts deterministic and fair. Let's see how it works.

---

**Navigate**: [← Previous: Evaluation Engine](04-evaluation-engine.md) | [Next: Bucketing Algorithm →](06-bucketing-algorithm.md)
