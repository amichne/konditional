package io.amichne.konditional.otel

import io.amichne.konditional.api.EvaluationResult
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.opentelemetry.api.trace.Span

/**
 * Evaluates a feature with OpenTelemetry instrumentation.
 *
 * Creates a trace span for the evaluation, records metrics with exemplar linking,
 * and propagates trace context. The span will be linked to the provided [parentSpan]
 * or the current active span if none is provided.
 *
 * ## Performance
 *
 * When tracing is disabled or sampling excludes this evaluation, overhead is minimal
 * (<1% latency impact). When sampled, expect ~5-10% overhead for span creation and
 * attribute population.
 *
 * ## Usage
 *
 * ```kotlin
 * // Using global telemetry instance
 * val enabled = myFlag.evaluateWithTelemetry(context)
 *
 * // With explicit telemetry instance
 * val enabled = myFlag.evaluateWithTelemetry(context, telemetry = customTelemetry)
 *
 * // With parent span
 * val enabled = myFlag.evaluateWithTelemetry(context, parentSpan = currentSpan)
 * ```
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use (defaults to global).
 * @param registry The registry to use (defaults to the feature's namespace).
 * @param parentSpan Optional parent span to link this evaluation to.
 * @return The evaluated value.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry = KonditionalTelemetry.global(),
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): T =
    telemetry.tracer.traceEvaluation(this, context, parentSpan) {
        explain(context, registry)
    }.value

/**
 * Evaluates a feature with OpenTelemetry instrumentation and returns detailed result.
 *
 * Like [evaluateWithTelemetry] but returns the full [EvaluationResult] for explainability.
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use (defaults to global).
 * @param registry The registry to use (defaults to the feature's namespace).
 * @param parentSpan Optional parent span to link this evaluation to.
 * @return The evaluation result with decision trace.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetryAndReason(
    context: C,
    telemetry: KonditionalTelemetry = KonditionalTelemetry.global(),
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): EvaluationResult<T> =
    telemetry.tracer.traceEvaluation(this, context, parentSpan) {
        explain(context, registry)
    }

/**
 * Convenience extension that auto-detects the current span from OpenTelemetry context.
 *
 * Equivalent to `evaluateWithTelemetry(context, telemetry, registry, parentSpan = Span.current())`.
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use (defaults to global).
 * @param registry The registry to use (defaults to the feature's namespace).
 * @return The evaluated value.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
    context: C,
    telemetry: KonditionalTelemetry = KonditionalTelemetry.global(),
    registry: NamespaceRegistry = namespace,
): T = evaluateWithTelemetry(context, telemetry, registry, parentSpan = Span.current())
