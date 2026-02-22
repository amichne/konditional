# OpenTelemetry reference

This page is the symbol-level API reference for `konditional-opentelemetry`.

## Read this page when

- You are wiring `KonditionalTelemetry` in application code.
- You need exact extension signatures and overload behavior.
- You are validating tracing and sampling contracts.

## API and contract reference

### Telemetry facade

```kotlin
class KonditionalTelemetry(
    otel: OpenTelemetry,
    val tracingConfig: TracingConfig = TracingConfig.DEFAULT,
    val metricsConfig: MetricsConfig = MetricsConfig.DEFAULT,
    instrumentationScope: String = "io.amichne.konditional",
) {
    val tracer: FlagEvaluationTracer
    val metrics: OtelMetricsCollector
    val logger: OtelLogger

    fun toRegistryHooks(): RegistryHooks

    companion object {
        fun install(telemetry: KonditionalTelemetry)
        fun global(): KonditionalTelemetry
        fun globalOrNull(): KonditionalTelemetry?
        fun noop(): KonditionalTelemetry
    }
}
```

### Evaluation extensions (preferred explicit telemetry path)

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): T

fun <T : Any, C : Context, M : Namespace>
Feature<T, C, M>.evaluateWithTelemetryAndReason(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): EvaluationDiagnostics<T>

fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
): T
```

Deprecated global-shim overloads remain available for compatibility and use
`KonditionalTelemetry.global()`.

### Tracing configuration

```kotlin
data class TracingConfig(
    val enabled: Boolean = true,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.PARENT_BASED,
    val includeContextAttributes: Boolean = true,
    val includeRuleDetails: Boolean = true,
    val sanitizePii: Boolean = true,
)

sealed interface SamplingStrategy {
    data object ALWAYS : SamplingStrategy
    data object NEVER : SamplingStrategy
    data object PARENT_BASED : SamplingStrategy
    data class RATIO(val percentage: Int) : SamplingStrategy
    data class FEATURE_FILTER(val predicate: (Feature<*, *, *>) -> Boolean)
        : SamplingStrategy
}
```

### Semantic attribute keys

- `feature.namespace`
- `feature.key`
- `feature.type`
- `evaluation.result.value`
- `evaluation.result.decision`
- `evaluation.duration_ns`
- `evaluation.config_version`
- `evaluation.rule.note`
- `evaluation.rule.specificity`
- `evaluation.bucket`
- `evaluation.ramp_up`
- `context.platform`
- `context.locale`
- `context.version`
- `context.stable_id.sha256_prefix`

## Deterministic API and contract notes

- `SamplingStrategy.RATIO` enforces `percentage in 0..100` at construction.
- For fixed telemetry config and input context, emitted span attributes are
  stable.
- Evaluation result semantics are unchanged by instrumentation: tracing wraps the
  same internal evaluation call.

## Canonical conceptual pages

- [Theory: Determinism proofs](/theory/determinism-proofs)
- [Theory: Atomicity guarantees](/theory/atomicity-guarantees)
- [Theory: Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

- [OpenTelemetry integration](/opentelemetry)
- [Observability reference](/observability/reference)
- [Feature evaluation API](/reference/api/feature-evaluation)
