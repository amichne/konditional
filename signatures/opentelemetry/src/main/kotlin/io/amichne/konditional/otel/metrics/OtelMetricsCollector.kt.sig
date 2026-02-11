file=opentelemetry/src/main/kotlin/io/amichne/konditional/otel/metrics/OtelMetricsCollector.kt
package=io.amichne.konditional.otel.metrics
imports=io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.core.ops.MetricsCollector,io.opentelemetry.api.common.AttributeKey,io.opentelemetry.api.common.Attributes,io.opentelemetry.api.metrics.LongCounter,io.opentelemetry.api.metrics.LongHistogram,io.opentelemetry.api.metrics.Meter,io.opentelemetry.context.Context
type=io.amichne.konditional.otel.metrics.OtelMetricsCollector|kind=class|decl=class OtelMetricsCollector( private val meter: Meter, private val config: MetricsConfig = MetricsConfig.DEFAULT, ) : MetricsCollector
fields:
- private val evaluationCounter: LongCounter
- private val evaluationDuration: LongHistogram
- private val configLoadCounter: LongCounter
- private val configRollbackCounter: LongCounter
methods:
- override fun recordEvaluation(event: Metrics.Evaluation)
- override fun recordConfigLoad(event: Metrics.ConfigLoadMetric)
- override fun recordConfigRollback(event: Metrics.ConfigRollbackMetric)
