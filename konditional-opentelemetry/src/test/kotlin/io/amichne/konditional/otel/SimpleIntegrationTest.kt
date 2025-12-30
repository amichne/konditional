package io.amichne.konditional.otel

import io.amichne.konditional.otel.traces.SamplingStrategy
import io.amichne.konditional.otel.traces.TracingConfig
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Basic integration tests for OpenTelemetry integration.
 *
 * These tests validate the core telemetry infrastructure without complex feature evaluation.
 */
class SimpleIntegrationTest {
    @Test
    fun `can create telemetry instance`() {
        val otel = OpenTelemetrySdk.builder().build()
        val telemetry = KonditionalTelemetry(otel = otel)

        assertNotNull(telemetry.tracer)
        assertNotNull(telemetry.metrics)
        assertNotNull(telemetry.logger)
    }

    @Test
    fun `noop telemetry works`() {
        val telemetry = KonditionalTelemetry.noop()
        assertNotNull(telemetry)
    }

    @Test
    fun `global instance can be installed and retrieved`() {
        val telemetry = KonditionalTelemetry.noop()
        KonditionalTelemetry.install(telemetry)

        val global = KonditionalTelemetry.global()
        assertEquals(telemetry, global)
    }

    @Test
    fun `toRegistryHooks creates valid hooks`() {
        val telemetry = KonditionalTelemetry.noop()
        val hooks = telemetry.toRegistryHooks()

        assertNotNull(hooks.logger)
        assertNotNull(hooks.metrics)
    }

    @Test
    fun `TracingConfig can be configured`() {
        val config =
            TracingConfig(
                enabled = true,
                samplingStrategy = SamplingStrategy.RATIO(50),
                includeContextAttributes = true,
                includeRuleDetails = true,
                sanitizePii = true,
            )

        assertEquals(true, config.enabled)
        assertEquals(SamplingStrategy.RATIO(50), config.samplingStrategy)
    }

    @Test
    fun `RATIO sampling validates percentage`() {
        try {
            SamplingStrategy.RATIO(150)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `telemetry integrates with OpenTelemetry SDK`() {
        val spanExporter = InMemorySpanExporter.create()
        val tracerProvider =
            SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build()

        val otel =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(tracerProvider)
                .build()

        val telemetry = KonditionalTelemetry(otel = otel)

        // Telemetry should be properly initialized
        assertNotNull(telemetry.tracer)
        assertNotNull(telemetry.metrics)
        assertNotNull(telemetry.logger)
    }
}
