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
- `constrain(...)` targets custom axes
- `extension { ... }` custom predicate
- `require(namedPredicate)` references a namespace-declared predicate
- `require { ... }` adds an inline, rule-scoped predicate
- `rampUp { ... }` percentage rollout
- `allowlist(...)` stable IDs that bypass ramp-up
- `note("...")` attaches a human-readable note
- `always()` / `matchAll()` mark a catch-all rule explicitly

All targeting calls inside one rule are combined with AND semantics. Repeating
`constrain(...)` calls for the same axis id widens allowed values with OR
semantics within that axis.

## Axis targeting and context values

Axis targeting resolves from the enum values you pass to `constrain(...)`.
The axis ID is derived from the enum fully-qualified class name by default.

```kotlin
import io.amichne.konditional.context.axis.KonditionalExplicitId

@KonditionalExplicitId("environment")
enum class Environment(override val id: String) : AxisValue<Environment> {
    PROD("prod"),
    STAGE("stage"),
}

enum class Tenant : AxisValue<Tenant> {
    ENTERPRISE,
}

object AppFeatures : Namespace("app") {
    val checkout by boolean<Context>(default = false) {
        rule(true) {
            constrain(Environment.PROD)
            constrain(Tenant.ENTERPRISE)
        }
    }
}
```

By default, an axis ID is derived from the enum fully-qualified class name.
Apply `@KonditionalExplicitId("...")` when you need a stable custom axis ID.

For context values, use `axes(...)`:

```kotlin
val values = axes(Environment.PROD)
```

## Targeting hierarchy

Konditional compiles rule criteria into a structural targeting tree.

- Each rule becomes a `Targeting.All` conjunction.
- Standard leaves represent locale, platform, version, and axis constraints.
- Each `extension { ... }` adds a `Targeting.Custom` leaf.
- `whenContext<R> { ... }` adds a guarded leaf that evaluates only when the
  runtime context implements `R`.

When a context lacks a required capability, guarded leaves return `false`
without throwing. This behavior replaces legacy flat predicate composition and
keeps rule matching deterministic.

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
`require(...)` is sugar over predicate references and inline custom predicates.

```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val subscriptionTier: Tier,
    val userRole: Role,
) : Context

enum class Tier { FREE, ENTERPRISE }
enum class Role { MEMBER, OWNER }

object EntitlementPredicates : Namespace() {
    val isPremium by predicate<EnterpriseContext> {
        subscriptionTier == Tier.ENTERPRISE
    }
}

val enterpriseOnly by boolean<EnterpriseContext>(default = false) {
    rule(true) {
        require(EntitlementPredicates.isPremium)
        require { userRole == Role.OWNER }
    }
}
```

<span id="claim-clm-pr02-01a"></span>
Named `require(predicateHandle)` and inline `require { ... }` predicates compose
with AND semantics within the same rule [CLM-PR02-01A].

- Multiple `extension { ... }` / inline `require { ... }` blocks on the same
  rule are combined with AND semantics.
- Each custom predicate call contributes predicate specificity.

### Capability narrowing with `whenContext`

Use `whenContext<R> { ... }` when a feature is defined on a broader context
type but a rule needs an additional capability:

```kotlin
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext

val enterpriseOnly by boolean<Context>(default = false) {
    rule(true) {
        whenContext<EnterpriseContext> {
            subscriptionTier == Tier.ENTERPRISE
        }
    }
}
```

- **Guarantee**: If runtime context is not `R`, the predicate returns `false`
  and does not throw.
- **Mechanism**: `whenContext` narrows context with a safe cast and evaluates
  the block only on success.
- **Boundary**: A rule using `whenContext<R>` never matches contexts that do
  not implement `R`.

The inline helper also works inside anonymous `require` predicates:

```kotlin
rule(true) {
    require {
        whenContext<EnterpriseContext> {
            subscriptionTier == Tier.ENTERPRISE
        }
    }
}
```

- **Guarantee**: Custom predicates participate in specificity ordering.

- **Mechanism**: Each `extension { ... }` and `whenContext<R> { ... }` call
  contributes one custom targeting leaf, and leaf specificities are summed.

- **Boundary**: Konditional does not validate predicate correctness or determinism.

## Reusable rule sets (`RuleSet`)

If you want to share a group of rules across multiple flags, you can build a
`RuleSet` and include it:

```kotlin
object AppFeatures : Namespace("app") {
    private val ruleTemplate by string<Context>(default = "v1")

    private val iosRollout = ruleTemplate.ruleSet {
        rule("v2") { ios() }
    }

    @KonditionalExplicitId("shared-rollout")
    private val sharedRollout by ruleSet<String, Context, Namespace> {
        rule("v2") { ios() }
    }

    val checkout by string<Context>(default = "v1") {
        include(iosRollout)
        include(sharedRollout)
        rule("v3") { rampUp { 10.0 } }
    }
}
```

<span id="claim-clm-pr02-01b"></span>
Namespace reusable rule sets are declaration-seeded by delegated property name.
Use `@KonditionalExplicitId` on the property to pin the seed across renames
[CLM-PR02-01B].

- Rule sets are included left-to-right; when two rules are equally specific, earlier included rules win.
- `RuleSet` supports composition via `+` to combine two sets while preserving ordering.

## Ramp-up allowlists

`allowlist(...)` bypasses the ramp-up check after the rule matches by criteria.

- **Boundary**: It does not override rule criteria, `isActive`, or the namespace kill-switch.

## Next steps

- [Core DSL best practices](/core/best-practices)
- [Evaluation model](/learn/evaluation-model)
- [Core API reference](/core/reference)

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
| --- | --- | --- | --- |
| `CLM-PR02-01A` | Named and inline `require` predicates compose with AND semantics. | [Custom predicates](#custom-predicates) | [Claims registry](/reference/claims-registry#clm-pr02-01a) |
| `CLM-PR02-01B` | Namespace reusable `ruleSet` declarations are delegate-seeded, with optional explicit seed pinning. | [Reusable rule sets](#reusable-rule-sets-ruleset) | [Claims registry](/reference/claims-registry#clm-pr02-01b) |
