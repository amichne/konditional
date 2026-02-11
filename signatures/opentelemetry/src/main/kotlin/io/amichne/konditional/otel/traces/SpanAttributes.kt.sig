file=opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/SpanAttributes.kt
package=io.amichne.konditional.otel.traces
imports=io.opentelemetry.api.common.AttributeKey
type=io.amichne.konditional.otel.traces.KonditionalSemanticAttributes|kind=object|decl=object KonditionalSemanticAttributes
type=io.amichne.konditional.otel.traces.DecisionType|kind=object|decl=object DecisionType
type=io.amichne.konditional.otel.traces.EventName|kind=object|decl=object EventName
fields:
- val FEATURE_NAMESPACE: AttributeKey<String>
- val FEATURE_KEY: AttributeKey<String>
- val FEATURE_TYPE: AttributeKey<String>
- val FEATURE_DEFAULT: AttributeKey<String>
- val EVALUATION_VALUE: AttributeKey<String>
- val EVALUATION_DECISION: AttributeKey<String>
- val EVALUATION_DURATION_NS: AttributeKey<Long>
- val EVALUATION_CONFIG_VERSION: AttributeKey<String>
- val EVALUATION_RULE_NOTE: AttributeKey<String>
- val EVALUATION_RULE_SPECIFICITY: AttributeKey<Long>
- val EVALUATION_BUCKET: AttributeKey<Long>
- val EVALUATION_RAMP_UP: AttributeKey<Double>
- val CONTEXT_PLATFORM: AttributeKey<String>
- val CONTEXT_LOCALE: AttributeKey<String>
- val CONTEXT_VERSION: AttributeKey<String>
- val CONTEXT_STABLE_ID_HASH: AttributeKey<String>
- const val DEFAULT
- const val RULE_MATCHED
- const val INACTIVE
- const val REGISTRY_DISABLED
- const val RULE_SKIPPED
