file=opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt
package=io.amichne.konditional.otel.traces
imports=io.amichne.konditional.api.EvaluationResult,io.amichne.konditional.context.Context,io.amichne.konditional.context.Context.LocaleContext,io.amichne.konditional.context.Context.PlatformContext,io.amichne.konditional.context.Context.StableIdContext,io.amichne.konditional.context.Context.VersionContext,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.features.Feature,io.amichne.konditional.otel.traces.KonditionalSemanticAttributes.DecisionType,io.opentelemetry.api.common.AttributeKey,io.opentelemetry.api.common.Attributes,io.opentelemetry.api.trace.Span,io.opentelemetry.api.trace.SpanKind,io.opentelemetry.api.trace.StatusCode,io.opentelemetry.api.trace.Tracer,io.opentelemetry.context.Context,java.util.concurrent.ThreadLocalRandom
type=io.amichne.konditional.otel.traces.FlagEvaluationTracer|kind=class|decl=class FlagEvaluationTracer( private val tracer: Tracer, private val config: TracingConfig, )
methods:
- fun <T : Any, C : Context> traceEvaluation( feature: Feature<T, C, *>, context: C, parentSpan: Span? = null, block: () -> EvaluationResult<T>, ): EvaluationResult<T>
- private fun <T : Any, C : Context> populateSpanFromResult( span: Span, result: EvaluationResult<T>, context: C, )
- private fun populateRuleDetails( span: Span, decision: EvaluationResult.Decision.Rule, )
- private fun addRuleSkippedEvent( span: Span, skipped: EvaluationResult.RuleMatch, )
- private fun shouldSample( feature: Feature<*, *, *>, context: Context, ): Boolean
- private fun <T : Any> sanitizeValue(value: T): String
- private fun EvaluationResult.Decision.toSpanValue(): String
