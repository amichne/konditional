---
title: Evaluate in App Code
sidebar_position: 4
---

# Evaluate in App Code

Evaluate a typed feature for one runtime context.

**Prerequisites:** You have completed [Define First Flag](/quickstart/define-first-flag).

<span id="claim-clm-pr01-09a"></span>
Feature evaluation follows core flag-definition semantics and deterministic bucketing behavior.

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId

val ctx = Context(
  locale = AppLocale.UNITED_STATES,
  platform = Platform.IOS,
  appVersion = Version.of(2, 1, 0),
  stableId = StableId.of("user-123"),
)

val variant: CheckoutVariant = AppFeatures.checkoutVariant.evaluate(ctx)
check(variant == CheckoutVariant.CLASSIC)
```

## Expected Outcome

After this step, evaluating your feature returns the declared default value for the supplied context.

## Next Steps

- [Concept: Evaluation Model](/concepts/evaluation-model) - Understand ordering and determinism boundaries.
- [Add Deterministic Ramp-Up](/quickstart/add-deterministic-ramp-up) - Add gradual rollout behavior.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-09A | Feature evaluation follows core flag-definition evaluation semantics and deterministic bucketing behavior. |
