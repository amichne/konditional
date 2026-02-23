---
title: Konditional
sidebar_position: 0
---

# Konditional

Typed, deterministic feature management for Kotlin applications with explicit JSON trust boundaries.

## Why Teams Adopt It

- Compile-time-safe declarations (`Namespace`, `Feature`, typed values).
- Deterministic evaluation and rollout behavior.
- Result-based boundary parsing with typed failure semantics.
- Namespace-scoped runtime operations and rollback controls.

## Fast Path

1. [Start Here](/overview/start-here)
2. [Quickstart](/quickstart/)
3. [First Success Map](/overview/first-success-map)

## Core Example

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId

enum class CheckoutVariant { CLASSIC, SMART }

object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}

val ctx = Context(
  locale = AppLocale.UNITED_STATES,
  platform = Platform.IOS,
  appVersion = Version.of(2, 0, 0),
  stableId = StableId.of("user-123"),
)

val variant = AppFeatures.checkoutVariant.evaluate(ctx)
```

## Navigate by Intent

- Understand architecture: [Concepts](/concepts/namespaces)
- Execute tasks: [Guides](/guides/remote-configuration)
- Look up exact contracts: [Reference](/reference/api-surface)
- Review guarantees: [Theory](/theory/type-safety-boundaries)
