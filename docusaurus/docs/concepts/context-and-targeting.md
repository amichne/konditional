---
title: Context and Targeting
sidebar_position: 4
---

# Context and Targeting

`Context` is the runtime evidence used to match rules.

## Standard Dimensions

- Locale (`Context.LocaleContext`)
- Platform (`Context.PlatformContext`)
- App version (`Context.VersionContext`)
- Stable ID (`Context.StableIdContext`)

## Custom Axes

Use `Axis<T>` + `AxisValue<T>` + `axisValues { ... }` for additional dimensions like tenant, region, or environment.

```kotlin
enum class Tier(override val id: String) : AxisValue<Tier> {
  FREE("free"), PRO("pro"), ENTERPRISE("enterprise")
}

object BillingFlags : Namespace("billing") {
  val tierAxis = axis<Tier>("tier")

  val premiumExport by boolean<Context>(default = false) {
    rule(true) { axis(tierAxis, Tier.PRO, Tier.ENTERPRISE) }
  }
}
```

## Boundary Rule

Treat context as runtime input: declaration is compile-time safe, but context values are still operational data and should be produced consistently by your app.

## Next Steps

- [Custom Targeting Axes Guide](/guides/custom-targeting-axes) - End-to-end axis declaration and usage.
- [Namespace Isolation Theory](/theory/namespace-isolation) - Why scope boundaries remain isolated.
