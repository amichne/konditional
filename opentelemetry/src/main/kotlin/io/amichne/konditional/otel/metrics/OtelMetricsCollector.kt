package io.amichne.konditional.otel.metrics

import io.amichne.konditional.core.ops.MetricsCollector
import io.amichne.konditional.core.ops.Metrics
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.context.Context as OtelContext

/**
 * OpenTelemetry implementation of [MetricsCollector].
 *
 * Records feature flag evaluation metrics with trace context linking via exemplars.
 * Designed for low-overhead operation on hot evaluation paths.
 *
 * @property meter OpenTelemetry meter instance.
 * @property config Metrics collection configuration.
 */
class OtelMetricsCollector(
    private val meter: Meter,
    private val config: MetricsConfig = MetricsConfig.DEFAULT,
) : MetricsCollector {
    private val evaluationCounter: LongCounter =
        meter
            .counterBuilder("feature.evaluation.count")
            .setDescription("Total number of feature flag evaluations")
            .build()

    private val evaluationDuration: LongHistogram =
        meter
            .histogramBuilder("feature.evaluation.duration")
            .setDescription("Feature flag evaluation duration in nanoseconds")
            .ofLongs()
            .build()

    private val configLoadCounter: LongCounter =
        meter
            .counterBuilder("feature.config.load.count")
            .setDescription("Total number of configuration loads")
            .build()

    private val configRollbackCounter: LongCounter =
        meter
            .counterBuilder("feature.config.rollback.count")
            .setDescription("Total number of configuration rollbacks")
            .build()

    override fun recordEvaluation(event: Metrics.Evaluation) {
        val attributes =
            Attributes
                .builder()
                .put(AttributeKey.stringKey("namespace"), event.namespaceId)
                .put(AttributeKey.stringKey("feature"), event.featureKey)
                .put(AttributeKey.stringKey("decision"), event.decision.name.lowercase())
                .put(AttributeKey.stringKey("mode"), event.mode.name.lowercase())
                .apply {
                    event.configVersion?.let { put(AttributeKey.stringKey("version"), it) }

                    if (config.includeRuleDetails) {
                        event.bucket?.let { put(AttributeKey.longKey("bucket"), it.toLong()) }
                        event.matchedRuleSpecificity?.let {
                            put(
                                AttributeKey.longKey("rule_specificity"),
                                it.toLong(),
                            )
                        }
                    }
                }.build()

        evaluationCounter.add(1, attributes, OtelContext.current())
        evaluationDuration.record(event.durationNanos, attributes, OtelContext.current())
    }

    override fun recordConfigLoad(event: Metrics.ConfigLoadMetric) {
        val attributes =
            Attributes
                .builder()
                .put(AttributeKey.stringKey("namespace"), event.namespaceId)
                .put(AttributeKey.longKey("feature_count"), event.featureCount.toLong())
                .apply {
                    event.version?.let { put(AttributeKey.stringKey("version"), it) }
                }.build()

        configLoadCounter.add(1, attributes, OtelContext.current())
    }

    override fun recordConfigRollback(event: Metrics.ConfigRollbackMetric) {
        val attributes =
            Attributes
                .builder()
                .put(AttributeKey.stringKey("namespace"), event.namespaceId)
                .put(AttributeKey.longKey("steps"), event.steps.toLong())
                .put(AttributeKey.booleanKey("success"), event.success)
                .apply {
                    event.version?.let { put(AttributeKey.stringKey("version"), it) }
                }.build()

        configRollbackCounter.add(1, attributes, OtelContext.current())
    }
}
