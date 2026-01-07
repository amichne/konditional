# Observability Reference

API reference for logging, metrics, and explainability: registry hooks, evaluation results, and deterministic bucketing.

---

## `RegistryHooks`

Dependency-free hooks for logging and metrics adapters.

```kotlin
data class RegistryHooks(
    val logger: KonditionalLogger = KonditionalLogger.NoOp,
    val metrics: MetricsCollector = MetricsCollector.NoOp,
)
```

Attach hooks to a namespace via `setHooks(...)`:

```kotlin
AppFeatures.setHooks(
    RegistryHooks.of(
        logger = myLogger,
        metrics = myMetrics,
    )
)
```

---

## `KonditionalLogger`

Minimal logging interface used by the runtime.

```kotlin
interface KonditionalLogger {
    fun debug(message: () -> String) {}
    fun info(message: () -> String) {}
    fun warn(message: () -> String, throwable: Throwable? = null) {}
    fun error(message: () -> String, throwable: Throwable? = null) {}
}
```

---

## `MetricsCollector`

Minimal metrics interface used by the runtime.

```kotlin
interface MetricsCollector {
    fun recordEvaluation(event: Metrics.Evaluation) {}
    fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}
    fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}
}
```

---

<details>
<summary>Advanced Options</summary>

## `EvaluationResult<T>`

Returned by `Feature.explain(...)`.

```kotlin
data class EvaluationResult<T : Any>(
    val namespaceId: String,
    val featureKey: String,
    val configVersion: String?,
    val mode: Metrics.Evaluation.EvaluationMode,
    val durationNanos: Long,
    val value: T,
    val decision: Decision,
)
```

---

## `BucketInfo` and `RampUpBucketing`

Deterministic ramp-up bucketing utilities (guaranteed to match runtime evaluation behavior).

```kotlin
object RampUpBucketing {
    fun bucket(stableId: StableId, featureKey: String, salt: String): Int
    fun explain(stableId: StableId, featureKey: String, salt: String, rampUp: RampUp): BucketInfo
}
```

---

## `ShadowMismatch<T>`

Result type passed to shadow mismatch callbacks.

```kotlin
data class ShadowMismatch<T : Any>(
    val featureKey: String,
    val baseline: EvaluationResult<T>,
    val candidate: EvaluationResult<T>,
    val kinds: Set<Kind>,
)
```

</details>

---

## Next steps

- [Shadow evaluation patterns](/observability/shadow-evaluation)
- [Core API reference](/core/reference)
