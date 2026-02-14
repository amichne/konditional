file=opentelemetry/src/main/kotlin/io/amichne/konditional/otel/KonditionalTelemetry.kt
package=io.amichne.konditional.otel
imports=io.amichne.konditional.core.ops.RegistryHooks,io.amichne.konditional.otel.logging.OtelLogger,io.amichne.konditional.otel.metrics.MetricsConfig,io.amichne.konditional.otel.metrics.OtelMetricsCollector,io.amichne.konditional.otel.traces.FlagEvaluationTracer,io.amichne.konditional.otel.traces.TracingConfig,io.opentelemetry.api.OpenTelemetry,org.jetbrains.annotations.TestOnly
type=io.amichne.konditional.otel.KonditionalTelemetry|kind=class|decl=class KonditionalTelemetry( otel: OpenTelemetry, val tracingConfig: TracingConfig = TracingConfig.DEFAULT, val metricsConfig: MetricsConfig = MetricsConfig.DEFAULT, instrumentationScope: String = DEFAULT_SCOPE, )
fields:
- val tracer: FlagEvaluationTracer
- val metrics: OtelMetricsCollector
- val logger: OtelLogger
methods:
- fun toRegistryHooks(): RegistryHooks
