@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.otel

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
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
 * // Preferred: explicit telemetry injection
 * val enabled = myFlag.evaluateWithTelemetry(context, telemetry = customTelemetry)
 *
 * // Optional parent span
 * val enabled = myFlag.evaluateWithTelemetry(
 *     context = context,
 *     telemetry = customTelemetry,
 *     parentSpan = currentSpan,
 * )
 * ```
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use.
 * @param registry The registry to use (defaults to the feature's namespace).
 * @param parentSpan Optional parent span to link this evaluation to.
 * @return The evaluated value.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): T =
    telemetry.tracer.traceEvaluation(this, context, parentSpan) {
        evaluateInternalApi(
            context = context,
            registry = registry,
            mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
        )
    }.value

/**
 * Deprecated global-telemetry compatibility shim for [evaluateWithTelemetry].
 *
 * Uses [KonditionalTelemetry.global] and preserves previous behavior for callers that
 * have installed process-wide telemetry via [KonditionalTelemetry.install].
 */
@Deprecated(
    message = "Global telemetry singleton path is deprecated. Pass telemetry explicitly.",
)
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): T =
    evaluateWithTelemetry(
        context = context,
        telemetry = KonditionalTelemetry.global(),
        registry = registry,
        parentSpan = parentSpan,
    )

/**
 * Evaluates a feature with OpenTelemetry instrumentation and returns detailed result.
 *
 * Like [evaluateWithTelemetry] but returns internal evaluation diagnostics for explainability.
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use.
 * @param registry The registry to use (defaults to the feature's namespace).
 * @param parentSpan Optional parent span to link this evaluation to.
 * @return The evaluation result with decision trace.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetryAndReason(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): EvaluationDiagnostics<T> =
    telemetry.tracer.traceEvaluation(this, context, parentSpan) {
        evaluateInternalApi(
            context = context,
            registry = registry,
            mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
        )
    }

/**
 * Deprecated global-telemetry compatibility shim for [evaluateWithTelemetryAndReason].
 *
 * Uses [KonditionalTelemetry.global] and preserves previous behavior for callers that
 * have installed process-wide telemetry via [KonditionalTelemetry.install].
 */
@Deprecated(
    message = "Global telemetry singleton path is deprecated. Pass telemetry explicitly.",
)
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetryAndReason(
    context: C,
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null,
): EvaluationDiagnostics<T> =
    evaluateWithTelemetryAndReason(
        context = context,
        telemetry = KonditionalTelemetry.global(),
        registry = registry,
        parentSpan = parentSpan,
    )

/**
 * Convenience extension that auto-detects the current span from OpenTelemetry context.
 *
 * Equivalent to `evaluateWithTelemetry(context, telemetry, registry, parentSpan = Span.current())`.
 *
 * @param context The evaluation context.
 * @param telemetry The telemetry instance to use.
 * @param registry The registry to use (defaults to the feature's namespace).
 * @return The evaluated value.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
    context: C,
    telemetry: KonditionalTelemetry,
    registry: NamespaceRegistry = namespace,
): T = evaluateWithTelemetry(context = context, telemetry = telemetry, registry = registry, parentSpan = Span.current())

/**
 * Deprecated global-telemetry compatibility shim for [evaluateWithAutoSpan].
 *
 * Uses [KonditionalTelemetry.global] and preserves previous behavior for callers that
 * have installed process-wide telemetry via [KonditionalTelemetry.install].
 */
@Deprecated(
    message = "Global telemetry singleton path is deprecated. Pass telemetry explicitly.",
)
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
    context: C,
    registry: NamespaceRegistry = namespace,
): T =
    evaluateWithAutoSpan(
        context = context,
        telemetry = KonditionalTelemetry.global(),
        registry = registry,
    )
