---
title: Custom Targeting Axes
sidebar_position: 4
---

# Custom Targeting Axes

Add domain-specific targeting dimensions such as tenant tier, region, or environment.

**Prerequisites:** You understand [Context and Targeting](/concepts/context-and-targeting).

## Step 1: Define Axis Values

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.KonditionalExplicitId

@KonditionalExplicitId("tenant-tier")
enum class TenantTier(override val id: String) : AxisValue<TenantTier> {
  FREE("free"), PRO("pro"), ENTERPRISE("enterprise")
}
```

By default, axis IDs are derived from the enum fully-qualified class name.
Apply `@KonditionalExplicitId` when you need a stable custom axis ID that will
survive enum package moves.

## Step 2: Target axis values in rules

```kotlin
object BillingFlags : Namespace("billing") {
  val premiumReporting by boolean<Context>(default = false) {
    rule(true) {
      constrain(TenantTier.PRO, TenantTier.ENTERPRISE)
    }
  }
}
```

## Step 3: Supply Axis Values in Context

```kotlin
import io.amichne.konditional.context.axis.axes

val values = axes(TenantTier.ENTERPRISE)
```

Attach those axis values in your context implementation so rules can resolve
them consistently.

## Expected Outcome

After this guide, your feature rules can target custom dimensions with compile-time axis typing.

## Next Steps

- [Theory: Namespace Isolation](/theory/namespace-isolation)
- [Guide: Namespace per Team](/guides/namespace-per-team)
