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

enum class TenantTier(override val id: String) : AxisValue<TenantTier> {
  FREE("free"), PRO("pro"), ENTERPRISE("enterprise")
}
```

## Step 2: Register Axis in Namespace

```kotlin
object BillingFlags : Namespace("billing") {
  val tenantTierAxis = axis<TenantTier>("tenant-tier")

  val premiumReporting by boolean<Context>(default = false) {
    rule(true) { axis(tenantTierAxis, TenantTier.PRO, TenantTier.ENTERPRISE) }
  }
}
```

## Step 3: Supply Axis Values in Context

```kotlin
import io.amichne.konditional.api.axisValues

val axisValues = axisValues(BillingFlags.axisCatalog) {
  set(BillingFlags.tenantTierAxis, TenantTier.ENTERPRISE)
}
```

Attach those axis values in your context implementation so rules can resolve them consistently.

## Expected Outcome

After this guide, your feature rules can target custom dimensions with compile-time axis typing.

## Next Steps

- [Theory: Namespace Isolation](/theory/namespace-isolation)
- [Guide: Namespace per Team](/guides/namespace-per-team)
