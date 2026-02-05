---
title: Rule Model (Targeting + Predicates)
---

# Rule Model (Targeting + Predicates)

Konditional models “if these conditions hold, yield this value” as a pairing:

- `Rule<C>`: targeting + an optional extension predicate (business logic)
- `ConditionalValue<T, C>`: the rule paired with a concrete value of type `T`

This separation keeps flags composable: you can build rule sets and include them in multiple flags without losing type
safety.

:::note Boundary reminder
Only base targeting is safe to configure remotely. Extension predicates are code and are not serialized.
:::

## What a rule can express

A rule has two conceptual layers:

1. **Base targeting** (serializable constraints): locale/platform/version/axis/stable-id constraints and rollout settings.
2. **Custom predicate** (non-serializable business logic): a strongly-typed predicate over your context type.

That’s why you’ll see DSL shapes like:

```kotlin
object Payments : Namespace("payments") {
    val applePayEnabled by boolean<Context>(default = false) {
        rule(true) { platforms(Platform.IOS) }
    }
}

data class EnterpriseContext(/* ... */) : Context /* + mixins */

object Premium : Namespace("premium") {
    val advancedAnalytics by boolean<EnterpriseContext>(default = false) {
        enable { extension { /* business logic */ } }
    }
}
```

**Guarantee / Mechanism / Boundary**

- **Guarantee (compile-time):** Rule values are the same type as the feature (`T`), and predicates receive the correct
  context type (`C`).
- **Mechanism:** `Feature<T, C, M>` binds the context type; `Rule<C>` and `ConditionalValue<T, C>` are generic in `C`.
- **Boundary:** Only the base targeting layer is safe to make remotely configurable; extension predicates are code.

## Targeting primitives (RuleScope)

Inside a rule block, you can express:

- `locales(...)` — locale targeting
- `platforms(...)` — platform targeting
- `versions { min(...); max(...) }` — version range targeting
- `axis(...)` — custom axis constraints
- `extension { ... }` — custom predicate over your context type
- `rampUp { ... }` — percentage rollout (deterministic)
- `allowlist(...)` — stable IDs that bypass ramp‑up
- `note("...")` — attach a human‑readable note
- `always()` / `matchAll()` — explicit catch‑all rule

:::tip Targeting stays type‑safe
Targeting operators are only available when the feature’s context type supports them.
If your context does not implement `Context.PlatformContext`, you can’t call `platforms(...)`.
:::

## Criteria‑first rules (`yields`)

For readability with complex values, you can declare criteria first and then yield a value:

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

- **Guarantee:** `rule { ... } yields value` is equivalent to `rule(value) { ... }`.
- **Boundary:** A criteria‑first `rule { ... }` must be completed with `yields(...)` (incomplete rules fail fast).

## Reusable rule sets (`RuleSet`)

Rule sets let you reuse a policy across multiple flags:

```kotlin
object AppFeatures : Namespace("app") {
    private val ruleTemplate by string<Context>(default = "v1")

    private val iosRollout = ruleTemplate.ruleSet {
        rule("v2") { platforms(Platform.IOS) }
    }

    val checkout by string<Context>(default = "v1") {
        include(iosRollout)
        rule("v3") { rampUp { 10.0 } }
    }
}
```

:::note Ordering policy
Rule sets are included left‑to‑right. When two rules are equally specific, earlier included rules win.
:::

## Ramp‑up allowlists

`allowlist(...)` bypasses ramp‑up after a rule matches by criteria.

:::caution Not a global bypass
Allowlists do not override rule criteria, `isActive`, or the namespace kill‑switch.
:::

## Specificity and precedence

Rules are sorted by a specificity score and evaluated from most-specific to least-specific. The intent is to make
“tighter targeting wins” a stable policy across the system.

When debugging, prefer `feature.explain(context)` so you can see:

- the matched rule (if any)
- specificity totals
- bucket info for rollout decisions

Next:

- [Context & Axes](/context-and-axes)
- [Rollouts & Bucketing](/rollouts-and-bucketing)
