# Observability reference

This page is the API-first reference for observability hooks, shadow options,
and mismatch contracts.

## Read this page when

- You are implementing `RegistryHooks` integrations.
- You need exact shadow evaluation signatures.
- You are consuming mismatch callbacks in production code.

## API and contract reference

### Hook container

```kotlin
@ConsistentCopyVisibility
data class RegistryHooks internal constructor(
    val logger: KonditionalLogger = KonditionalLogger.NoOp,
    val metrics: MetricsCollector = MetricsCollector.NoOp,
) {
    companion object {
        val None: RegistryHooks
        fun of(
            logger: KonditionalLogger = KonditionalLogger.NoOp,
            metrics: MetricsCollector = MetricsCollector.NoOp,
        ): RegistryHooks
    }
}
```

### Logger and metrics hook interfaces

```kotlin
interface KonditionalLogger {
    fun debug(message: () -> String) {}
    fun info(message: () -> String) {}
    fun warn(message: () -> String, throwable: Throwable? = null) {}
    fun error(message: () -> String, throwable: Throwable? = null) {}

    data object NoOp : KonditionalLogger
}

interface MetricsCollector {
    fun recordEvaluation(event: Metrics.Evaluation) {}
    fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {}
    fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {}

    data object NoOp : MetricsCollector
}
```

### Shadow APIs

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
): T

fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
    context: C,
    candidateRegistry: NamespaceRegistry,
    baselineRegistry: NamespaceRegistry = namespace,
    options: ShadowOptions = ShadowOptions.defaults(),
    onMismatch: (ShadowMismatch<T>) -> Unit = {},
)
```

### Shadow options and mismatch payload

```kotlin
data class ShadowOptions internal constructor(
    val reportDecisionMismatches: Boolean,
    val evaluateCandidateWhenBaselineDisabled: Boolean,
) {
    companion object {
        fun defaults(): ShadowOptions
        fun of(
            reportDecisionMismatches: Boolean = false,
            evaluateCandidateWhenBaselineDisabled: Boolean = false,
        ): ShadowOptions
    }
}

data class ShadowMismatch<T : Any> internal constructor(
    val featureKey: String,
    val baseline: EvaluationDiagnostics<T>,
    val candidate: EvaluationDiagnostics<T>,
    val kinds: Set<Kind>,
) {
    enum class Kind { VALUE, DECISION }
}
```

## Deterministic API and contract notes

- `evaluateWithShadow(...)` always returns the baseline evaluation value.
- Candidate evaluation is skipped when the baseline kill-switch is enabled,
  unless `evaluateCandidateWhenBaselineDisabled` is `true`.
- Decision mismatches are reported only when
  `reportDecisionMismatches = true`.
- Hook callbacks run inline and must be deterministic to avoid adding
  nondeterministic latency.

## Canonical conceptual pages

- [Theory: Migration and shadowing](/theory/migration-and-shadowing)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [Theory: Namespace isolation](/theory/namespace-isolation)

## Next steps

- [Shadow evaluation reference](/observability/shadow-evaluation)
- [Feature evaluation API](/reference/api/feature-evaluation)
- [OpenTelemetry reference](/opentelemetry/reference)
