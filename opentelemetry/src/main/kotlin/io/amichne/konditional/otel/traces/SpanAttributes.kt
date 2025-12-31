package io.amichne.konditional.otel.traces

import io.opentelemetry.api.common.AttributeKey

/**
 * Semantic conventions for Konditional feature flag evaluation spans.
 *
 * Follows OpenTelemetry semantic conventions where applicable, with Konditional-specific
 * attributes prefixed with `feature.` and `evaluation.`.
 */
object KonditionalSemanticAttributes {
    // Feature identity
    val FEATURE_NAMESPACE: AttributeKey<String> = AttributeKey.stringKey("feature.namespace")
    val FEATURE_KEY: AttributeKey<String> = AttributeKey.stringKey("feature.key")
    val FEATURE_TYPE: AttributeKey<String> = AttributeKey.stringKey("feature.type")
    val FEATURE_DEFAULT: AttributeKey<String> = AttributeKey.stringKey("feature.default")

    // Evaluation result
    val EVALUATION_VALUE: AttributeKey<String> = AttributeKey.stringKey("evaluation.result.value")
    val EVALUATION_DECISION: AttributeKey<String> = AttributeKey.stringKey("evaluation.result.decision")
    val EVALUATION_DURATION_NS: AttributeKey<Long> = AttributeKey.longKey("evaluation.duration_ns")
    val EVALUATION_CONFIG_VERSION: AttributeKey<String> = AttributeKey.stringKey("evaluation.config_version")

    // Rule matching
    val EVALUATION_RULE_NOTE: AttributeKey<String> = AttributeKey.stringKey("evaluation.rule.note")
    val EVALUATION_RULE_SPECIFICITY: AttributeKey<Long> = AttributeKey.longKey("evaluation.rule.specificity")
    val EVALUATION_BUCKET: AttributeKey<Long> = AttributeKey.longKey("evaluation.bucket")
    val EVALUATION_RAMP_UP: AttributeKey<Double> = AttributeKey.doubleKey("evaluation.ramp_up")

    // Context (sanitized for cardinality)
    val CONTEXT_PLATFORM: AttributeKey<String> = AttributeKey.stringKey("context.platform")
    val CONTEXT_LOCALE: AttributeKey<String> = AttributeKey.stringKey("context.locale")
    val CONTEXT_VERSION: AttributeKey<String> = AttributeKey.stringKey("context.version")
    val CONTEXT_STABLE_ID_HASH: AttributeKey<String> = AttributeKey.stringKey("context.stable_id.sha256_prefix")

    // Decision types (constants for EVALUATION_DECISION attribute)
    object DecisionType {
        const val DEFAULT = "default"
        const val RULE_MATCHED = "rule_matched"
        const val INACTIVE = "inactive"
        const val REGISTRY_DISABLED = "registry_disabled"
    }

    // Event names
    object EventName {
        const val RULE_SKIPPED = "rule_skipped"
    }
}
