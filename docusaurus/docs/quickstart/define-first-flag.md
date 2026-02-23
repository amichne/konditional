---
title: Define First Flag
sidebar_position: 3
---

# Define First Flag

Declare one namespace-owned typed feature with a default value.

**Prerequisites:** You have completed [Install](/quickstart/install).

<span id="claim-clm-pr01-08a"></span>
Feature declarations are represented by `Namespace`, `Feature`, and `FlagDefinition` types.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

enum class CheckoutVariant { CLASSIC, SMART }

object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}
```

<span id="claim-clm-pr01-08b"></span>
Namespace declarations define the compile-time schema used by boundary codecs and runtime snapshot loaders.

## Verify

```kotlin
val featureKey = AppFeatures.checkoutVariant.key
check(featureKey == "checkoutVariant")
```

## Expected Outcome

After this step, a typed feature declaration compiles and is accessible through your namespace object.

## Next Steps

- [Concept: Namespaces](/concepts/namespaces) - Understand namespace boundaries.
- [Concept: Features and Types](/concepts/features-and-types) - Deep dive into typed feature values.
- [Evaluate in App Code](/quickstart/evaluate-in-app-code) - Evaluate the declaration with context.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-08A | Feature declarations are represented by Namespace, Feature, and FlagDefinition types. |
| CLM-PR01-08B | Namespaces provide a compiled schema used by boundary codecs and loaders. |
