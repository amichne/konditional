---
title: Start Here
sidebar_position: 1
---

# Start Here

Konditional is a Kotlin-first feature/configuration framework for teams that want compile-time typing and deterministic evaluation behavior.

<span id="claim-clm-pr01-01a"></span>
Feature declarations are modeled through namespace-owned typed feature definitions.

<span id="claim-clm-pr01-01b"></span>
Runtime configuration ingestion is exposed through a snapshot loader that returns `Result` and preserves typed parse failures.

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
  appVersion = Version.of(1, 0, 0),
  stableId = StableId.of("user-123"),
)

val variant: CheckoutVariant = AppFeatures.checkoutVariant.evaluate(ctx)
```

## Who This Fits

Konditional is a fit when your team wants typed declarations in code, deterministic rollouts, and explicit JSON boundary handling instead of dynamic string-key control planes.

## Next Steps

- [Quickstart](/quickstart/) - Build the first end-to-end working path in about 15 minutes.
- [Why Typed Flags](/overview/why-typed-flags) - See concrete failure modes this model prevents.
- [First Success Map](/overview/first-success-map) - Choose your route based on your immediate goal.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-01A | Konditional models feature declarations through namespace-owned typed feature definitions. |
| CLM-PR01-01B | Runtime configuration ingestion is exposed through a snapshot loader that returns `Result` and supports typed parse failures. |
