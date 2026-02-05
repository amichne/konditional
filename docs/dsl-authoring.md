---
title: DSL Surface (Authoring Rules)
---

# DSL Surface (Authoring Rules)

The core DSL is designed to make “the safe thing” the easiest thing:

- Values are always typed (`T`).
- Targeting is only expressible when the context supports it (`C`).
- Rollout is explicit (`RampUp`, salt, allowlists).

The two primary authoring scopes are:

- `FlagScope<T, C, M>`: defines rules, default behavior, activation, salt/allowlists, and composition.
- `RuleScope<C>`: defines targeting criteria and yields a value (`T`).

| Scope        | Responsibilities                                                                 | Compile-time guarantee |
|--------------|----------------------------------------------------------------------------------|------------------------|
| `FlagScope`  | Defaults, rules, activation, salt/allowlists, rule set composition                | `T`, `C`, `M` bound    |
| `RuleScope`  | Targeting criteria + optional custom predicate (business logic)                   | `C` enforced           |

:::note Guardrails are intentional
The DSL intentionally limits what you can express to avoid ambiguous or order-dependent behavior.
:::

## A minimal flag definition

```kotlin
object Payments : Namespace("payments") {
    val applePayEnabled by boolean<Context>(default = false) {
        // A rule is a (targeting + optional predicate) yielding a typed value.
        rule(true) { platforms(Platform.IOS) }
    }
}
```

## Flag-level controls

Typical `FlagScope` controls include:

- `rule(...) { ... }` — add a rule
- `include(ruleSet)` — include reusable rule sets
- `salt("v2")` — resample population deterministically
- `allowlist(...)` — stable IDs that bypass ramp‑up
- `active(false)` — temporarily deactivate a flag (returns default)

:::caution Activation vs kill‑switch
`active(false)` deactivates a single flag definition; the kill‑switch disables an entire namespace.
:::

## Composition (rule sets)

For larger organizations, the DSL is built to support reusing consistent targeting policies:

- a “tenant safety policy” applied to every flag
- a “platform support matrix” rule set
- a “country allowlist” rule set

The guiding principle is: composition should not force declaration ordering policy. Precedence is based on specificity.

Next:

- [Rules](/rules)
- [FAQ](/faq) for “why precedence isn’t declaration order”
