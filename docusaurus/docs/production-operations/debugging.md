# Operational debugging

This page shows production-safe debugging techniques that match the current
public API surface. The core evaluation API is `evaluate(...)`; there is no
public `Feature.explain(...)` API.

## Overview

Use these tools depending on the question you need to answer.

1. Value verification with controlled contexts (`evaluate(...)`)
2. Deterministic rollout inspection (`RampUpBucketing`)
3. Runtime boundary diagnostics (`NamespaceSnapshotLoader` + typed parse errors)
4. Migration comparison without behavior drift (`evaluateWithShadow`)

## Verify returned values with controlled contexts

Start by evaluating the same feature against a small matrix of deterministic
contexts. This isolates rule-targeting and ramp-up assumptions.

```kotlin
val contexts = listOf(
    Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-a"),
    ),
    Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version.of(2, 1, 0),
        stableId = StableId.of("user-b"),
    ),
)

contexts.forEach { ctx ->
    val value = AppFeatures.checkoutVariant.evaluate(ctx)
    println("platform=${ctx.platform.id} stableId=${ctx.stableId.id} value=$value")
}
```

When values are stable in this matrix, drift usually comes from upstream
context construction or config ingestion.

## Inspect deterministic bucketing directly

Use `RampUpBucketing` when you need to answer, "why is this user outside a
rollout percentage?"

```kotlin
import io.amichne.konditional.api.RampUpBucketing

val bucket = RampUpBucketing.explain(
    stableId = StableId.of("user-123"),
    featureKey = AppFeatures.darkMode.key,
    salt = "v1",
    rampUp = RampUp.of(10.0),
)

println("bucket=${bucket.bucket}")
println("thresholdBasisPoints=${bucket.thresholdBasisPoints}")
println("inRollout=${bucket.inRollout}")
```

The output is deterministic for the same `(stableId, featureKey, salt)` tuple.

## Debug configuration load failures at the boundary

`NamespaceSnapshotLoader.load(...)` returns `Result` and never partially applies
an invalid snapshot.

```kotlin
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

val result = NamespaceSnapshotLoader(AppFeatures).load(json)
if (result.isFailure) {
    when (val error = result.parseErrorOrNull()) {
        is ParseError.InvalidJson -> println("Invalid JSON: ${error.reason}")
        is ParseError.FeatureNotFound -> println("Unknown feature key: ${error.key}")
        is ParseError.InvalidSnapshot -> println("Schema/type mismatch: ${error.reason}")
        else -> println("Load failed: ${result.exceptionOrNull()?.message}")
    }
}
```

On failure, runtime behavior remains on the last-known-good configuration.

## Add hooks for low-overhead operational visibility

Attach logger and metrics hooks at the namespace registry level.

```kotlin
import io.amichne.konditional.core.ops.KonditionalLogger
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.ops.MetricsCollector
import io.amichne.konditional.core.ops.RegistryHooks

AppFeatures.setHooks(
    RegistryHooks.of(
        logger = object : KonditionalLogger {
            override fun warn(message: () -> String, throwable: Throwable?) {
                println("WARN ${message()}")
            }
        },
        metrics = object : MetricsCollector {
            override fun recordEvaluation(event: Metrics.Evaluation) {
                println("evaluation key=${event.featureKey} decision=${event.decision}")
            }
        },
    ),
)
```

Keep hook implementations lightweight because they run on hot paths.

## Compare baseline and candidate behavior with shadow evaluation

For migrations, use `evaluateWithShadow(...)` from
`konditional-observability`. Baseline behavior remains authoritative.

```kotlin
import io.amichne.konditional.api.evaluateWithShadow

val value = AppFeatures.checkoutVariant.evaluateWithShadow(
    context = ctx,
    baselineRegistry = baselineRegistry,
    candidateRegistry = candidateRegistry,
    onMismatch = { mismatch ->
        println("shadow mismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
    },
)
```

Your app uses `value` from baseline evaluation while mismatches are reported as
observability signals.

## Next steps

- [Core DSL best practices](/core/best-practices)
- [Thread safety](/production-operations/thread-safety)
- [Failure modes](/production-operations/failure-modes)
- [Shadow evaluation](/observability/shadow-evaluation)
