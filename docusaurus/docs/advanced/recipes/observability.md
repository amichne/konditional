# Lightweight Observability Hooks

Attach logging and metrics without depending on a specific vendor SDK.

```kotlin
fun attachHooks() {
    val hooks =
        RegistryHooks.of(
            logger =
                object : KonditionalLogger {
                    override fun warn(message: () -> String, throwable: Throwable?) {
                        AppLogger.warn(message(), throwable)
                    }
                },
            metrics =
                object : MetricsCollector {
                    override fun recordEvaluation(event: Metrics.Evaluation) {
                        AppMetrics.increment("konditional.eval", tags = mapOf("feature" to event.featureKey))
                    }
                },
        )

    AppFeatures.setHooks(hooks)
}
```

- **Guarantee**: Hooks receive evaluation and lifecycle signals with consistent payloads.
- **Mechanism**: `RegistryHooks` are invoked inside the runtime's evaluation and load paths.
- **Boundary**: Hooks run on the hot path; keep them non-blocking.

---
