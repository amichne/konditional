package io.amichne.konditional.otel.traces

import io.amichne.konditional.core.features.Feature

/**
 * Configuration for tracing feature flag evaluations.
 *
 * @property enabled Global toggle for tracing.
 * @property samplingStrategy Determines which evaluations to trace.
 * @property includeContextAttributes Include sanitized context attributes (platform, locale, etc.).
 * @property includeRuleDetails Include detailed rule matching information.
 * @property sanitizePii Truncate/hash PII-sensitive values (stable IDs, custom context axes).
 */
data class TracingConfig(
    val enabled: Boolean = true,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.PARENT_BASED,
    val includeContextAttributes: Boolean = true,
    val includeRuleDetails: Boolean = true,
    val sanitizePii: Boolean = true,
) {
    companion object {
        val DEFAULT = TracingConfig()
    }
}

/**
 * Sampling strategies for controlling trace volume.
 *
 * - [ALWAYS]: Trace all evaluations (high overhead, use for testing).
 * - [NEVER]: Disable tracing entirely.
 * - [PARENT_BASED]: Sample only if parent span is sampled (recommended for production).
 * - [RATIO]: Sample a percentage of evaluations deterministically based on stable ID.
 * - [FEATURE_FILTER]: Sample based on feature predicate (e.g., trace only specific namespaces).
 */
sealed interface SamplingStrategy {
    data object ALWAYS : SamplingStrategy
    data object NEVER : SamplingStrategy
    data object PARENT_BASED : SamplingStrategy

    /**
     * Sample based on deterministic ratio.
     *
     * @property percentage Percentage of evaluations to sample (0-100).
     */
    data class RATIO(val percentage: Int) : SamplingStrategy {
        init {
            require(percentage in 0..100) { "Sampling percentage must be 0-100, got $percentage" }
        }
    }

    /**
     * Sample based on feature predicate.
     *
     * @property predicate Function that determines if a feature should be traced.
     */
    data class FEATURE_FILTER(val predicate: (Feature<*, *, *>) -> Boolean) : SamplingStrategy
}
