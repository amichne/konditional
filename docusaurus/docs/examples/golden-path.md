---
title: Golden Path Example
---

# Golden Path: end-to-end feature flag

This example shows the current, public workflow from definition to evaluation
using APIs that are exposed today.

## What this shows

- Namespace and feature definitions
- Rule targeting and deterministic ramp-up
- Typed context construction
- Public `evaluate(...)` usage
- Bucketing diagnostics with `RampUpBucketing`

## Complete code

```kotlin
// File: src/main/kotlin/com/example/GoldenPath.kt
package com.example

import io.amichne.konditional.api.RampUpBucketing
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.*
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable

enum class CheckoutVariant { CLASSIC, FAST_PATH, EXPERIMENTAL }

object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false) {
        enable {
            ios()
            note("iOS users get dark mode")
        }
        enable {
            rampUp { 10.0 }
            note("10% gradual rollout")
        }
    }

    val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.FAST_PATH) {
            ios()
            versions { min(2, 0, 0) }
            rampUp { 25.0 }
            note("iOS 2.0+ users, 25% rollout")
        }
        rule(CheckoutVariant.EXPERIMENTAL) {
            android()
            rampUp { 5.0 }
            note("Android users, 5% rollout")
        }
    }
}

fun buildContext(userId: String, platform: Platform, version: Version): Context =
    Context(
        locale = AppLocale.UNITED_STATES,
        platform = platform,
        appVersion = version,
        stableId = StableId.of(userId),
    )

fun main() {
    val iosUser = buildContext("user-123", Platform.IOS, Version.of(2, 1, 0))
    val androidUser = buildContext("user-456", Platform.ANDROID, Version.of(1, 5, 0))

    val iosDarkMode: Boolean = AppFeatures.darkMode.evaluate(iosUser)
    val iosCheckout: CheckoutVariant = AppFeatures.checkoutVariant.evaluate(iosUser)

    val androidDarkMode: Boolean = AppFeatures.darkMode.evaluate(androidUser)
    val androidCheckout: CheckoutVariant = AppFeatures.checkoutVariant.evaluate(androidUser)

    println("iOS -> darkMode=$iosDarkMode, checkout=$iosCheckout")
    println("Android -> darkMode=$androidDarkMode, checkout=$androidCheckout")

    // Optional deterministic rollout introspection for one feature
    val bucket = RampUpBucketing.explain(
        stableId = iosUser.stableId,
        featureKey = AppFeatures.darkMode.key,
        salt = "v1",
        rampUp = RampUp.of(10.0),
    )
    println("darkMode bucket=${bucket.bucket}, inRollout=${bucket.inRollout}")
}
```

## Key concepts demonstrated

| Concept | Location | What it shows |
|---------|----------|---------------|
| Namespace isolation | `object AppFeatures : Namespace("app")` | One ownership boundary for related features |
| Typed feature definitions | `val darkMode by boolean<Context>(...)` | Compile-time value type and required default |
| Rule targeting | `ios()`, `android()`, `versions { ... }` | Deterministic matching criteria |
| Ramp-up rollout | `rampUp { ... }` | Stable bucketing via `stableId` |
| Total evaluation | `evaluate(...)` | Returns a typed value (no nullable result wrapper) |
| Debugging helper | `RampUpBucketing.explain(...)` | Deterministic bucket introspection |

## Next steps

- [Install and setup guide](/quickstart/install)
- [Core DSL best practices](/core/best-practices)
- [Evaluation model](/learn/evaluation-model)
- [Runtime operations](/runtime/operations)
