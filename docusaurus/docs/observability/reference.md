# Observability Reference

API reference for logging, metrics, and shadow mismatch reporting.

## Registry Hooks

```kotlin
data class RegistryHooks(
    val logger: KonditionalLogger = KonditionalLogger.NoOp,
    val metrics: MetricsCollector = MetricsCollector.NoOp,
)
```

## Logger and Metrics Interfaces

```kotlin
interface KonditionalLogger {
    fun debug(message: () -> String) {}
    fun info(message: () -> String) {}
    fun warn(message: () -> String, throwable: Throwable? = null) {}
    fun error(message: () -> String, throwable: Throwable? = null) {}
}

interface MetricsCollector {
    fun recordEvaluation(event: Metrics.Evaluation) {}
    fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}
    fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}
}
```

## Shadow Mismatch

```kotlin
data class ShadowMismatch<T : Any>(
    val featureKey: String,
    val baseline: EvaluationDiagnostics<T>,
    val candidate: EvaluationDiagnostics<T>,
    val kinds: Set<Kind>,
)

enum class Kind { VALUE, DECISION }
```

`EvaluationDiagnostics` is an internal opt-in diagnostics type used by sibling modules.
