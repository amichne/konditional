file=opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt
package=io.amichne.konditional.otel.traces
imports=io.amichne.konditional.context.Context,io.amichne.konditional.context.Context.LocaleContext,io.amichne.konditional.context.Context.PlatformContext,io.amichne.konditional.context.Context.StableIdContext,io.amichne.konditional.context.Context.VersionContext,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.features.Feature,io.amichne.konditional.internal.evaluation.EvaluationDiagnostics,io.amichne.konditional.otel.traces.KonditionalSemanticAttributes.DecisionType,io.opentelemetry.api.common.AttributeKey,io.opentelemetry.api.common.Attributes,io.opentelemetry.api.trace.Span,io.opentelemetry.api.trace.SpanKind,io.opentelemetry.api.trace.StatusCode,io.opentelemetry.api.trace.Tracer,io.opentelemetry.context.Context,java.util.concurrent.ThreadLocalRandom
type=io.amichne.konditional.otel.traces.FlagEvaluationTracer|kind=class|decl=class FlagEvaluationTracer( private val tracer: Tracer, private val config: TracingConfig, )
methods:
- fun <T : Any, C : Context> traceEvaluation( feature: Feature<T, C, *>, context: C, parentSpan: Span? = null, block: () -> EvaluationDiagnostics<T>, ): EvaluationDiagnostics<T>
- private fun <T : Any, C : Context> populateSpanFromResult( span: Span, result: EvaluationDiagnostics<T>, context: C, )
- private fun populateRuleDetails( span: Span, decision: EvaluationDiagnostics.Decision.Rule, )
- private fun addRuleSkippedEvent( span: Span, skipped: EvaluationDiagnostics.RuleMatch, )
- private fun shouldSample( feature: Feature<*, *, *>, context: Context, ): Boolean
- private fun <T : Any> sanitizeValue(value: T): String
- private fun EvaluationDiagnostics.Decision.toSpanValue(): String
