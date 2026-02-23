---
title: Verify End-to-End
sidebar_position: 7
---

# Verify End-to-End

Run a compact assertion checklist before shipping.

**Prerequisites:** You have completed [Load First Snapshot Safely](/quickstart/load-first-snapshot-safely).

<span id="claim-clm-pr01-12a"></span>
End-to-end verification relies on deterministic bucketing, typed boundary parse failures, and namespace-scoped runtime operations.

## Assertions

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

enum class CheckoutVariant { CLASSIC, SMART }

object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}

object RecommendationsFeatures : Namespace("recommendations") {
  val useModelB by boolean<Context>(default = false)
}

val ctx = Context(
  locale = AppLocale.UNITED_STATES,
  platform = Platform.IOS,
  appVersion = Version.of(2, 1, 0),
  stableId = StableId.of("user-123"),
)

// 1) Determinism: same context -> same result
val a = AppFeatures.checkoutVariant.evaluate(ctx)
val b = AppFeatures.checkoutVariant.evaluate(ctx)
check(a == b)

// 2) Boundary rejection: invalid JSON fails with typed parse error
val invalidJson = """{ "flags": [ { "key": "bad" } ] }"""
val load = NamespaceSnapshotLoader(AppFeatures).load(invalidJson)
check(load.isFailure)
check(load.parseErrorOrNull() != null)

// 3) Namespace independence: operations on app namespace do not mutate recommendations namespace
val recsBefore = RecommendationsFeatures.useModelB.evaluate(ctx)
AppFeatures.disableAll()
val recsAfter = RecommendationsFeatures.useModelB.evaluate(ctx)
check(recsBefore == recsAfter)
```

## Formal Guarantee Links

- [Type Safety Boundaries](/theory/type-safety-boundaries)
- [Determinism Proofs](/theory/determinism-proofs)
- [Namespace Isolation](/theory/namespace-isolation)
- [Parse Don't Validate](/theory/parse-dont-validate)

## Expected Outcome

After this step, you have executable checks proving deterministic evaluation, safe boundary rejection, and namespace isolation.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-12A | End-to-end verification relies on deterministic bucketing, boundary parse types, and namespace runtime operations. |
