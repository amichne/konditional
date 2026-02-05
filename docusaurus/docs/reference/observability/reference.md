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

## `ShadowOptions`

Controls how shadow evaluation behaves.

```kotlin
data class ShadowOptions(
    val reportDecisionMismatches: Boolean,
    val evaluateCandidateWhenBaselineDisabled: Boolean,
)
```

- Construct via `ShadowOptions.defaults()` (conservative) or `ShadowOptions.of(...)` (explicit).
- `reportDecisionMismatches`: when `true`, mismatch callbacks can include `ShadowMismatch.Kind.DECISION` when the baseline and candidate decision types differ.
- `evaluateCandidateWhenBaselineDisabled`: when `true`, evaluates the candidate even if the baseline registry kill-switch is enabled.

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

enum class Kind { VALUE, DECISION }
```

</details>

---

## Next steps

- [Shadow evaluation patterns](/reference/observability/shadow-evaluation)
- [Core API reference](/reference/core/reference)
