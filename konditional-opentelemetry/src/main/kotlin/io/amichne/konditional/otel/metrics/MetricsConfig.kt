package io.amichne.konditional.otel.metrics

/**
 * Configuration for OpenTelemetry metrics collection.
 *
 * @property includeRuleDetails Include detailed rule attributes (bucket, specificity) in metrics.
 *   Disabling this reduces cardinality at the cost of granularity.
 */
data class MetricsConfig(
    val includeRuleDetails: Boolean = true,
) {
    companion object {
        val DEFAULT = MetricsConfig()
    }
}
