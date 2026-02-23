---
title: Load First Snapshot Safely
sidebar_position: 6
---

# Load First Snapshot Safely

Load JSON through a typed boundary and keep runtime state safe on failures.

**Prerequisites:** You have completed [Add Deterministic Ramp-Up](/quickstart/add-deterministic-ramp-up).

<span id="claim-clm-pr01-11a"></span>
Snapshot loading exposes `Result`-based ingestion with codec-backed decode and load options.

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

val ctx = Context(
  locale = AppLocale.UNITED_STATES,
  platform = Platform.IOS,
  appVersion = Version.of(2, 1, 0),
  stableId = StableId.of("user-123"),
)

val goodJson = """{ "flags": [] }"""
val badJson = """{ "flags": [ { "key": "bad" } ] }"""

val loader = NamespaceSnapshotLoader(AppFeatures)

val goodResult = loader.load(goodJson)
check(goodResult.isSuccess)

val before = AppFeatures.checkoutVariant.evaluate(ctx)

val badResult = loader.load(badJson)
check(badResult.isFailure)
```

<span id="claim-clm-pr01-11b"></span>
Boundary failures surface as `ParseError` values wrapped by `KonditionalBoundaryFailure`.

```kotlin
val parseError = badResult.parseErrorOrNull()
check(parseError != null)

val after = AppFeatures.checkoutVariant.evaluate(ctx)
check(before == after) // last-known-good remains active
```

## Expected Outcome

After this step, valid JSON updates runtime state while invalid JSON is rejected without crashing or partially applying.

## Next Steps

- [Concept: Parse Boundary](/concepts/parse-boundary) - Understand the boundary contract in detail.
- [Verify End-to-End](/quickstart/verify-end-to-end) - Run a full guarantee checklist.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-11A | Snapshot loading API exposes Result-based ingestion with options and codec-backed decode. |
| CLM-PR01-11B | Boundary failures are modeled as ParseError values wrapped by KonditionalBoundaryFailure. |
