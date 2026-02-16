@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.otel.traces

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.LocaleContext
import io.amichne.konditional.context.Context.PlatformContext
import io.amichne.konditional.context.Context.StableIdContext
import io.amichne.konditional.context.Context.VersionContext
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.otel.traces.KonditionalSemanticAttributes.DecisionType
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import java.util.concurrent.ThreadLocalRandom
import io.opentelemetry.context.Context as OtelContext

// Only first 8 characters of stable ID hash for PII safety
private const val DIGITS_FROM_VAL = 8

/**
 * Creates and populates OpenTelemetry spans for feature flag evaluations.
 *
 * Low-overhead tracer with configurable sampling strategies to minimize performance impact
 * on hot evaluation paths.
 *
 * @property tracer OpenTelemetry tracer instance.
 * @property config Tracing configuration including sampling strategy.
 */
class FlagEvaluationTracer(
    private val tracer: Tracer,
    private val config: TracingConfig,
) {
    /**
     * Traces a feature evaluation operation.
     *
     * Creates a span with semantic attributes from the evaluation result. Handles parent
     * span propagation and sampling according to configured strategy.
     *
     * @param feature The feature being evaluated.
     * @param context The evaluation context.
     * @param parentSpan Optional parent span to link this evaluation to.
     * @param block The evaluation operation to trace.
     * @return The evaluated value.
     */
    fun <T : Any, C : Context> traceEvaluation(
        feature: Feature<T, C, *>,
        context: C,
        parentSpan: Span? = null,
        block: () -> EvaluationDiagnostics<T>,
    ): EvaluationDiagnostics<T> {
        if (!config.enabled) return block()

        if (!shouldSample(feature, context)) return block()

        val spanBuilder =
            tracer
                .spanBuilder("feature.evaluation")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(KonditionalSemanticAttributes.FEATURE_KEY, feature.key)
                .setAttribute(KonditionalSemanticAttributes.FEATURE_NAMESPACE, feature.namespace.id)

        val span =
            parentSpan
                ?.let { spanBuilder.setParent(OtelContext.current().with(it)).startSpan() }
                ?: spanBuilder.startSpan()

        return try {
            OtelContext.current().with(span).makeCurrent().use {
                val result = block()

                populateSpanFromResult(span, result, context)

                result
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "Evaluation failed")
            throw e
        } finally {
            span.end()
        }
    }

    private fun <T : Any, C : Context> populateSpanFromResult(
        span: Span,
        result: EvaluationDiagnostics<T>,
        context: C,
    ) {
        // Result value (sanitized for cardinality)
        span.setAttribute(KonditionalSemanticAttributes.EVALUATION_VALUE, sanitizeValue(result.value))
        span.setAttribute(KonditionalSemanticAttributes.EVALUATION_DECISION, result.decision.toSpanValue())
        span.setAttribute(KonditionalSemanticAttributes.EVALUATION_DURATION_NS, result.durationNanos)

        result.configVersion?.let {
            span.setAttribute(KonditionalSemanticAttributes.EVALUATION_CONFIG_VERSION, it)
        }

        if (config.includeContextAttributes) {
            (context as? PlatformContext)?.platform?.id?.let {
                span.setAttribute(KonditionalSemanticAttributes.CONTEXT_PLATFORM, it)
            }
            (context as? LocaleContext)?.locale?.id?.let {
                span.setAttribute(KonditionalSemanticAttributes.CONTEXT_LOCALE, it)
            }
            (context as? VersionContext)?.appVersion?.let {
                span.setAttribute(KonditionalSemanticAttributes.CONTEXT_VERSION, it.toString())
            }

            if (config.sanitizePii) {
                (context as? StableIdContext)?.stableId?.hexId?.id?.take(DIGITS_FROM_VAL)?.let {
                    span.setAttribute(
                        KonditionalSemanticAttributes.CONTEXT_STABLE_ID_HASH,
                        it,
                    )
                }
            }
        }

        if (config.includeRuleDetails) {
            when (val decision = result.decision) {
                is EvaluationDiagnostics.Decision.Rule -> populateRuleDetails(span, decision)
                is EvaluationDiagnostics.Decision.Default ->
                    decision.skippedByRollout?.let {
                        addRuleSkippedEvent(
                            span,
                            it,
                        )
                    }
                else -> { /* No additional attributes */
                }
            }
        }
    }

    private fun populateRuleDetails(
        span: Span,
        decision: EvaluationDiagnostics.Decision.Rule,
    ) {
        val matched = decision.matched

        matched.rule.note?.let {
            span.setAttribute(KonditionalSemanticAttributes.EVALUATION_RULE_NOTE, it)
        }

        span.setAttribute(
            KonditionalSemanticAttributes.EVALUATION_RULE_SPECIFICITY,
            matched.rule.totalSpecificity.toLong(),
        )

        span.setAttribute(
            KonditionalSemanticAttributes.EVALUATION_BUCKET,
            matched.bucket.bucket.toLong(),
        )

        span.setAttribute(
            KonditionalSemanticAttributes.EVALUATION_RAMP_UP,
            matched.rule.rollout.value,
        )

        decision.skippedByRollout?.let { addRuleSkippedEvent(span, it) }
    }

    private fun addRuleSkippedEvent(
        span: Span,
        skipped: EvaluationDiagnostics.RuleMatch,
    ) {
        span.addEvent(
            KonditionalSemanticAttributes.EventName.RULE_SKIPPED,
            Attributes.of(
                AttributeKey.stringKey("rule_note"),
                skipped.rule.note ?: "unnamed",
                AttributeKey.stringKey("reason"),
                "ramp_up_excluded",
                AttributeKey.longKey("bucket"),
                skipped.bucket.bucket.toLong(),
                AttributeKey.doubleKey("ramp_up"),
                skipped.rule.rollout.value,
            ),
        )
    }

    private fun shouldSample(
        feature: Feature<*, *, *>,
        context: Context,
    ): Boolean =
        when (config.samplingStrategy) {
            SamplingStrategy.ALWAYS -> true
            SamplingStrategy.NEVER -> false
            SamplingStrategy.PARENT_BASED -> Span.current().spanContext.isSampled
            is SamplingStrategy.RATIO -> {
                val percentage = config.samplingStrategy.percentage
                val deterministicHash =
                    (context as? StableIdContext)?.stableId?.hexId?.id?.hashCode()
                        ?: context.axisValues
                            .takeIf { it != AxisValues.EMPTY }
                            ?.hashCode()
                val deterministicSample = deterministicHash?.let { hash ->
                    (hash.toLong() and 0x7FFFFFFF) % 100 < percentage
                }
                // Deterministic sampling when available; otherwise fall back to probabilistic sampling.
                deterministicSample ?: (ThreadLocalRandom.current().nextInt(100) < percentage)
            }
            is SamplingStrategy.FEATURE_FILTER -> config.samplingStrategy.predicate(feature)
        }

    private fun <T : Any> sanitizeValue(value: T): String =
        when (value) {
            is Boolean -> value.toString()
            is Number -> value.toString()
            is Enum<*> -> value.name
            is String -> if (value.length > 50) "${value.take(47)}..." else value
            else -> value::class.simpleName ?: "unknown"
        }

    private fun EvaluationDiagnostics.Decision.toSpanValue(): String =
        when (this) {
            is EvaluationDiagnostics.Decision.Default -> DecisionType.DEFAULT
            is EvaluationDiagnostics.Decision.Rule -> DecisionType.RULE_MATCHED
            is EvaluationDiagnostics.Decision.Inactive -> DecisionType.INACTIVE
            is EvaluationDiagnostics.Decision.RegistryDisabled -> DecisionType.REGISTRY_DISABLED
        }
}
