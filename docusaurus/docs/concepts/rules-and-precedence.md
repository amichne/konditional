---
title: Rules and Precedence
sidebar_position: 3
---

# Rules and Precedence

Rules define conditional values. Evaluation selects one outcome deterministically for each input context.

## Matching Model

A rule can constrain locale, platform, app version, stable-ID ramp-up, and custom axes.

```kotlin
object CheckoutFlags : Namespace("checkout") {
  val newCheckout by boolean<Context>(default = false) {
    rule(true) {
      locales(AppLocale.UNITED_STATES)
      platforms(Platform.IOS)
      rampUp { 25 }
      note("US iOS rollout")
    }
  }
}
```

## Precedence

- Higher-specificity rules are evaluated before lower-specificity rules.
- If no rule matches, the declared default is returned.
- For equivalent specificity, declaration order remains stable and deterministic.

## Why This Matters

The precedence model makes behavior explainable in reviews and reproducible in tests.

## Next Steps

- [Context and Targeting](/concepts/context-and-targeting) - Understand each targeting dimension.
- [Determinism Proofs](/theory/determinism-proofs) - Formal reasoning for stable outcomes.
