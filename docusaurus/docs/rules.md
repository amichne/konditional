---
title: Rule Model (Targeting + Predicates)
---

# Rule Model (Targeting + Predicates)

Konditional models “if these conditions hold, yield this value” as a pairing:

- `Rule<C>`: targeting + an optional extension predicate (business logic)
- `ConditionalValue<T, C>`: the rule paired with a concrete value of type `T`

This separation keeps flags composable: you can build rule sets and include them in multiple flags without losing type
safety.

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

## Specificity and precedence

Rules are sorted by a specificity score and evaluated from most-specific to least-specific. The intent is to make
“tighter targeting wins” a stable policy across the system.

When debugging, prefer `feature.explain(context)` so you can see:

- the matched rule (if any)
- specificity totals
- bucket info for rollout decisions

Next:

- [Context & Axes](context-and-axes)
- [Rollouts & Bucketing](rollouts-and-bucketing)

