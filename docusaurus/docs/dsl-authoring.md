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

## A minimal flag definition

```kotlin
object Payments : Namespace("payments") {
    val applePayEnabled by boolean<Context>(default = false) {
        // A rule is a (targeting + optional predicate) yielding a typed value.
        rule(true) { platforms(Platform.IOS) }
    }
}
```

## Composition (rule sets)

For larger organizations, the DSL is built to support reusing consistent targeting policies:

- a “tenant safety policy” applied to every flag
- a “platform support matrix” rule set
- a “country allowlist” rule set

The guiding principle is: composition should not force declaration ordering policy. Precedence is based on specificity.

Next:

- [Rules](rules)
- [FAQ](faq) for “why precedence isn’t declaration order”

