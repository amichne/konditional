---
name: documentation
description: Write precise, mechanically-grounded Konditional documentation that distinguishes compile-time guarantees from runtime behavior
---

# Konditional Documentation

## Instructions

### Use Precise, Bounded Claims
State what's guaranteed and under what conditions:

**Bad**: "Konditional is type-safe"
**Good**: "Type safety is enforced at compile-time for statically-defined features. Dynamically-loaded configurations are validated at the deserialization boundary."

**Template**:
```markdown
**Guarantee**: [What's guaranteed]
**Mechanism**: [How it's enforced]
**Boundary**: [Where it applies / doesn't apply]
```

### Distinguish Compile-Time vs Runtime
```markdown
| Aspect | Guarantee Level | Mechanism |
|--------|-----------------|-----------|
| Property access | Compile-time | Property delegation |
| Return types | Compile-time | Generic type propagation |
| Rule values | Compile-time | Constrained DSL |
| Non-null returns | Compile-time | Required defaults |
| JSON configuration | Runtime validation | `ParseResult` boundary |
| Business logic correctness | Not guaranteed | Human responsibility |
```

### Ground Claims in Mechanisms
**Bad**: "Rules are evaluated in order"
**Good**: "Rules are sorted by specificity (number of targeting criteria) before evaluation. Higher specificity rules are evaluated first."

**Bad**: "Rollouts are consistent"
**Good**: "Rollouts use SHA-256 hashing of `($salt:$flagKey:${stableId.hexId})` to deterministically assign users to buckets [0, 9999]. The same inputs always produce the same bucket."

### Consistent Terminology
| Term | Use | Don't Use |
|------|-----|-----------|
| Feature | Typed configuration value with rules | Flag, setting, config |
| FeatureContainer | Object holding features | Registry, config class |
| Context | Runtime evaluation inputs | Environment, request |
| Rule | Criteria → value mapping | Condition, constraint |
| Specificity | Number of targeting criteria | Priority, weight |
| Bucketing | SHA-256 user assignment | Hashing, randomization |

### Provide Runnable Examples
```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.*

object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
    }
}

val context = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("user-123")
)

val result: Boolean = AppFeatures.darkMode.evaluate(context)
// Result: true
// Why: iOS platform matches rule
```

## Examples

### Documenting Deterministic Rollouts
```markdown
## Deterministic Rollouts

**Guarantee**: Same user, flag, and salt combination always produces the same bucket assignment.

**Mechanism**:
1. Hash input: `SHA-256("$salt:$flagKey:${stableId.hexId}")`
2. Reduce to bucket: `hash % 10000` (range [0, 9999])
3. Compare to threshold: `bucket < (percentage * 100).toInt()`

**Implications**:
- Increasing 10% → 20% only adds users (existing stay in)
- Changing `salt` redistributes all buckets (use for resets)
- Different flags have independent buckets (`flagKey` in hash)

**Example**:
```kotlin
val feature by boolean(default = false) {
    salt("v1")
    rule(true) { rollout { 30.0 } }
}

// User A: hash("v1:feature:userA") % 10000 = 2500 → in rollout (< 3000)
// User B: hash("v1:feature:userB") % 10000 = 4500 → not in (>= 3000)
// User A will ALWAYS be in rollout unless salt <changes></changes>
```


```

### Documenting Type Safety
```markdown
## Type Safety Guarantees

### Compile-Time (Static Definitions)
**What's Guaranteed**:
- Property access checked (typos → compile error)
- Return types enforced (type mismatches → compile error)
- Rule values validated (wrong types → compile error)
- Non-null returns (defaults required)

**Mechanism**: Property delegation with generic type parameters
```kotlin
val darkMode by boolean<Context>(default = false)
val enabled: Boolean = AppFeatures.darkMode.evaluate(ctx)
val wrong: String = AppFeatures.darkMode.evaluate(ctx)  // Compile error
```

### Runtime (JSON Configuration)
**What's Guaranteed**:
- JSON validation before configuration loads
- Type mismatches rejected at parse boundary
- Invalid updates don't affect running system

**Mechanism**: Explicit `ParseResult` handling
```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> namespace.load(result.value)
    is ParseResult.Failure -> logError(result.error)
}
```

### Not Guaranteed
- Semantic correctness of business rules
- Appropriate rollout percentages
- Correct targeting logic
```

### Documenting Rule Precedence
```markdown
## Rule Precedence

**Mechanism**: Rules are sorted by **specificity** (number of targeting criteria) before evaluation.

**Specificity Calculation**:
- Count non-rollout criteria in each rule
- Sort descending (highest specificity first)
- Evaluate rules in sorted order

**Example**:
```kotlin
val feature by boolean(default = false) {
    // Evaluated in this order (specificity 3 → 2 → 1 → 0):
    rule(true) { platforms(IOS); locales(US); versions { min(2,0,0) } }  // 3
    rule(true) { platforms(IOS); locales(US) }                           // 2
    rule(true) { platforms(IOS) }                                        // 1
    rule(true) { rollout { 50.0 } }                                      // 0
}

// Context: iOS + France + v2.1.0
// - Rule 1: iOS✓ France✗ v2.1.0✓ → skip (not all match)
// - Rule 2: iOS✓ France✗ → skip
// - Rule 3: iOS✓ → return true
```

**Key Points**:
- Rollout percentage does NOT affect specificity
- All criteria must match (AND, not OR)
- First matching rule that passes rollout wins
```

### Common Mistakes

**Mistake 1: Abstract claims without mechanisms**
**Wrong**: "Konditional is deterministic"
**Right**: "Evaluation is deterministic because SHA-256 hashing of `($salt:$flagKey:$id)` always produces the same bucket for the same inputs"

**Mistake 2: Overstating guarantees**
**Wrong**: "Configuration is always correct"
**Right**: "JSON configuration is validated at parse time. Semantic correctness requires human review."

**Mistake 3: Hedging when certainty exists**
**Wrong**: "Type safety is generally enforced in most cases"
**Right**: "Type safety is enforced at compile-time for statically-defined features"

**Mistake 4: Incomplete examples**
**Wrong**: `val feature by boolean(default = false)`
**Right**:
```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val feature by boolean<Context>(default = false)
}

val result = Features.feature.evaluate(context)
```
