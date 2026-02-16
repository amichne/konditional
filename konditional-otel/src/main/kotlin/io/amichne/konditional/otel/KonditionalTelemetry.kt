package io.amichne.konditional.otel

import io.amichne.konditional.core.ops.RegistryHooks
import io.amichne.konditional.otel.logging.OtelLogger
import io.amichne.konditional.otel.metrics.MetricsConfig
import io.amichne.konditional.otel.metrics.OtelMetricsCollector
import io.amichne.konditional.otel.traces.FlagEvaluationTracer
import io.amichne.konditional.otel.traces.TracingConfig
import io.opentelemetry.api.OpenTelemetry
import org.jetbrains.annotations.TestOnly

/**
 * Facade for OpenTelemetry integration with Konditional.
 *
 * Provides centralized configuration for tracing, metrics, and logging. Can be installed
 * globally or used per-evaluation.
 *
 * ## Usage
 *
 * ```kotlin
 * // Setup
 * val otel = OpenTelemetrySdk.builder()
 *     .setTracerProvider(tracerProvider)
 *     .setMeterProvider(meterProvider)
 *     .setLoggerProvider(loggerProvider)
 *     .build()
 *
 * val telemetry = KonditionalTelemetry(
 *     otel = otel,
 *     tracingConfig = TracingConfig(
 *         samplingStrategy = SamplingStrategy.RATIO(10)
 *     )
 * )
 *
 * // Preferred: pass telemetry explicitly at call sites
 * val value = myFlag.evaluateWithTelemetry(context = context, telemetry = telemetry)
 *
 * // Compatibility: install globally for deprecated global-shim overloads
 * KonditionalTelemetry.install(telemetry)
 * ```
 *
 * @property otel OpenTelemetry SDK instance.
 * @property tracingConfig Configuration for trace span creation.
 * @property metricsConfig Configuration for metrics collection.
 * @property instrumentationScope Scope name for telemetry signals (defaults to "io.amichne.konditional").
 */
class KonditionalTelemetry(
    otel: OpenTelemetry,
    val tracingConfig: TracingConfig = TracingConfig.DEFAULT,
    val metricsConfig: MetricsConfig = MetricsConfig.DEFAULT,
    instrumentationScope: String = DEFAULT_SCOPE,
) {
    /**
     * Tracer for creating evaluation spans.
     */
    val tracer: FlagEvaluationTracer =
        FlagEvaluationTracer(
            tracer = otel.getTracer(instrumentationScope),
            config = tracingConfig,
        )

    /**
     * Metrics collector for recording evaluation metrics.
     */
    val metrics: OtelMetricsCollector =
        OtelMetricsCollector(
            meter = otel.getMeter(instrumentationScope),
            config = metricsConfig,
        )

    /**
     * Logger for structured logging with trace context.
     */
    val logger: OtelLogger =
        OtelLogger(
            logger = otel.logsBridge.get(instrumentationScope),
        )

    /**
     * Converts this telemetry instance to [RegistryHooks] for use with namespace registries.
     */
    fun toRegistryHooks(): RegistryHooks =
        RegistryHooks.of(
            logger = logger,
            metrics = metrics,
        )

    companion object {
        private const val DEFAULT_SCOPE = "io.amichne.konditional"

        @Volatile
        private var globalInstance: KonditionalTelemetry? = null

        /**
         * Installs a global telemetry instance.
         *
         * This instance is consumed by deprecated global-shim telemetry extension overloads.
         * Prefer passing telemetry explicitly at call sites instead.
         *
         * @param telemetry The telemetry instance to install globally.
         */
        fun install(telemetry: KonditionalTelemetry) {
            globalInstance = telemetry
        }

        /**
         * Retrieves the global telemetry instance.
         *
         * @throws IllegalStateException if no global instance has been installed.
         */
        fun global(): KonditionalTelemetry =
            globalInstance ?: throw IllegalStateException(
                "No global KonditionalTelemetry installed. Call KonditionalTelemetry.install() first.",
            )

        /**
         * Retrieves the global telemetry instance if installed, otherwise null.
         */
        fun globalOrNull(): KonditionalTelemetry? = globalInstance

        /**
         * Clears the global telemetry instance.
         *
         * Intended only for deterministic test isolation.
         */
        @TestOnly
        internal fun resetGlobalForTests() {
            globalInstance = null
        }

        /**
         * Creates a no-op telemetry instance for testing.
         */
        fun noop(): KonditionalTelemetry =
            KonditionalTelemetry(
                otel = OpenTelemetry.noop(),
                tracingConfig = TracingConfig(enabled = false),
            )
    }
}
