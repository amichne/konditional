package io.amichne.konditional.core.ops

/**
 * Operational hooks for observability.
 *
 * These hooks are intentionally dependency-free:
 * - no logging framework dependency (callers can bridge to SLF4J, Log4j, etc.)
 * - no metrics framework dependency (callers can bridge to Micrometer, OpenTelemetry, etc.)
 */
@ConsistentCopyVisibility
data class RegistryHooks internal constructor(
    val logger: KonditionalLogger = KonditionalLogger.NoOp,
    val metrics: MetricsCollector = MetricsCollector.NoOp,
) {
    companion object {
        val None: RegistryHooks = RegistryHooks()
        fun of(
            logger: KonditionalLogger = KonditionalLogger.NoOp,
            metrics: MetricsCollector = MetricsCollector.NoOp,
        ): RegistryHooks = RegistryHooks(logger, metrics)
    }
}

@ConsistentCopyVisibility
data class EvaluationMetric internal constructor(
    val namespaceId: String,
    val featureKey: String,
    val mode: EvaluationMode,
    val durationNanos: Long,
    val decision: DecisionKind,
    val configVersion: String? = null,
    val bucket: Int? = null,
    val matchedRuleSpecificity: Int? = null,
) {
    enum class EvaluationMode { NORMAL, EXPLAIN, SHADOW }

    enum class DecisionKind { DEFAULT, RULE, INACTIVE, REGISTRY_DISABLED }
}

@ConsistentCopyVisibility
data class ConfigLoadMetric internal constructor(
    val namespaceId: String,
    val featureCount: Int,
    val version: String? = null,
) {
    companion object {
        fun of(namespaceId: String, featureCount: Int, version: String?): ConfigLoadMetric =
            ConfigLoadMetric(namespaceId, featureCount, version)
    }
}

@ConsistentCopyVisibility
data class ConfigRollbackMetric internal constructor(
    val namespaceId: String,
    val steps: Int,
    val success: Boolean,
    val version: String? = null,
)
