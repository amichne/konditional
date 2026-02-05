# Rule DSL Reference

This page documents the core rule-building DSL available in `konditional-core`.

## Rule basics

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
        rule(true) { locales(AppLocale.UNITED_STATES) }
    }
}
```

## Boolean sugar

```kotlin
val darkMode by boolean<Context>(default = false) {
    enable { ios() }
    disable { android() }
}
```

## Targeting primitives

Inside a rule block (`RuleScope`):

- `locales(...)` targets locale ids
- `platforms(...)` targets platform ids
- `versions { min(...); max(...) }` targets version ranges
- `axis(...)` targets custom axes
- `extension { ... }` custom predicate
- `rampUp { ... }` percentage rollout
- `allowlist(...)` stable IDs that bypass ramp-up
- `note("...")` attaches a human-readable note
- `always()` / `matchAll()` mark a catch-all rule explicitly

### Example

```kotlin
val checkout by string<Context>(default = "v1") {
    rule("v2") {
        platforms(Platform.IOS)
        versions { min(3, 0, 0) }
        rampUp { 25.0 }
        note("iOS v2 rollout")
    }
}
```

## Criteria-first rules (`yields`)

For readability (especially with complex values), you can declare criteria first and then yield a value:

```kotlin
val checkout by string<Context>(default = "v1") {
    rule {
        platforms(Platform.IOS)
        versions { min(3, 0, 0) }
        rampUp { 25.0 }
        note("iOS v2 rollout")
    } yields "v2"
}
```

- **Guarantee**: `rule { ... } yields value` is equivalent to `rule(value) { ... }`.
- **Boundary**: A criteria-first `rule { ... }` must always be completed with `yields(...)` (unclosed rules fail fast at definition time).

## Custom predicates

`extension { ... }` receives the `Context` type for the feature.

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: Tier,
) : Context

val enterpriseOnly by boolean<EnterpriseContext>(default = false) {
    rule(true) { extension { subscriptionTier == Tier.ENTERPRISE } }
}
```

- **Guarantee**: Custom predicates participate in specificity ordering.

- **Mechanism**: Predicate implementations report their own `specificity()` which is added to the rule total.

- **Boundary**: Konditional does not validate predicate correctness or determinism.

## Reusable rule sets (`RuleSet`)

If you want to share a group of rules across multiple flags, you can build a `RuleSet` and include it:

```kotlin
object AppFeatures : Namespace("app") {
    private val ruleTemplate by string<Context>(default = "v1")

    private val iosRollout = ruleTemplate.ruleSet {
        rule("v2") { ios() }
    }

    val checkout by string<Context>(default = "v1") {
        include(iosRollout)
        rule("v3") { rampUp { 10.0 } }
    }
}
```

- Rule sets are included left-to-right; when two rules are equally specific, earlier included rules win.
- `RuleSet` supports composition via `+` to combine two sets while preserving ordering.

## Ramp-up allowlists

`allowlist(...)` bypasses the ramp-up check after the rule matches by criteria.

- **Boundary**: It does not override rule criteria, `isActive`, or the namespace kill-switch.

## Next steps

- [Evaluation model](/learn/evaluation-model)
- [Core API reference](/core/reference)
