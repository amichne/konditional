---
title: Quick Start (Minimal Example)
---

# Quick Start (Minimal Example)

This is the smallest end-to-end workflow:

1. Define a namespace.
2. Define a flag (typed, with a default).
3. Evaluate it for a context.
4. Explain the decision when debugging.

:::tip Minimal on purpose
This is the shortest path from definition → evaluation → explanation. 
It is intentionally small; you can add richer targeting and rollouts once the shape is familiar.
:::

```kotlin

object Payments : Namespace("payments") {
    val applePayEnabled by boolean<Context>(default = false) {
        enabled() { ios() }
    }
}

val context = object : Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext {
        override val locale = AppLocale.UNITED_STATES
        override val platform = Platform.IOS
        override val appVersion = Version.of(2, 1, 0)
        override val stableId = StableId.of("user-123")
    }

val enabled: Boolean = Payments.applePayEnabled.evaluate(context)
val explanation = Payments.applePayEnabled.explain(context)
```

**What is compile-time vs runtime here?**

- **Compile-time**: The flag is `Feature<Boolean, Context, Payments>` (typed value + typed context + namespace binding).
- **Runtime**: The evaluated value depends on registry state (kill-switch), the active definition, and rule matching.

Next:

- [Core Concepts](core-concepts)
- [Evaluation Flow](evaluation-flow)
