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
import io.amichne.konditional.context.axis.KonditionalExplicitId

@KonditionalExplicitId("tier")
enum class Tier(override val id: String) : AxisValue<Tier> {
  FREE("free"), PRO("pro"), ENTERPRISE("enterprise")
}

object BillingFlags : Namespace("billing") {
  val tierAxis = axis<Tier>()

  val premiumExport by boolean<Context>(default = false) {
    rule(true) {
      variant {
        tierAxis { include(Tier.PRO, Tier.ENTERPRISE) }
      }
    }
  }
}
```

Axis IDs default to the enum fully-qualified class name. Use
`@KonditionalExplicitId("...")` when you need a stable custom axis ID for
persisted rule payloads.

## Boundary Rule

Treat context as runtime input: declaration is compile-time safe, but context values are still operational data and should be produced consistently by your app.

## Next Steps

- [Custom Targeting Axes Guide](/guides/custom-targeting-axes) - End-to-end axis declaration and usage.
- [Namespace Isolation Theory](/theory/namespace-isolation) - Why scope boundaries remain isolated.
