@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.otel

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.otel.traces.KonditionalSemanticAttributes
import io.amichne.konditional.otel.traces.SamplingStrategy
import io.amichne.konditional.otel.traces.TracingConfig
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for telemetry extension APIs with explicit injection and deprecated global compatibility shims.
 */
class FeatureTelemetryExtensionsTest {
    @BeforeTest
    fun setUp() {
        KonditionalTelemetry.resetGlobalForTests()
    }

    @AfterTest
    fun tearDown() {
        KonditionalTelemetry.resetGlobalForTests()
    }

    @Test
    fun `explicit telemetry overloads work without global installation`() {
        val namespace = TestNamespace("otel-explicit")
        val probe = telemetryProbe()

        try {
            val context = testContext("explicit")

            val value = namespace.enabled.evaluateWithTelemetry(context = context, telemetry = probe.telemetry)
            val explained = namespace.enabled.evaluateWithTelemetryAndReason(context = context, telemetry = probe.telemetry)
            val withAutoSpan = namespace.enabled.evaluateWithAutoSpan(context = context, telemetry = probe.telemetry)

            assertEquals(true, value)
            assertEquals(true, explained.value)
            assertEquals(true, withAutoSpan)
            assertEquals(3, probe.spanExporter.finishedSpanItems.size)
        } finally {
            probe.close()
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated global shim overloads preserve installed-global behavior`() {
        val namespace = TestNamespace("otel-global-compatible")
        val probe = telemetryProbe()

        try {
            KonditionalTelemetry.install(probe.telemetry)
            val context = testContext("global")

            val value = namespace.enabled.evaluateWithTelemetry(context = context)
            val explained = namespace.enabled.evaluateWithTelemetryAndReason(context = context)
            val withAutoSpan = namespace.enabled.evaluateWithAutoSpan(context = context)

            assertEquals(true, value)
            assertEquals(true, explained.value)
            assertEquals(true, withAutoSpan)
            assertEquals(3, probe.spanExporter.finishedSpanItems.size)
        } finally {
            probe.close()
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated global shim overloads still fail when no global telemetry is installed`() {
        val namespace = TestNamespace("otel-global-missing")
        val context = testContext("missing-global")

        assertFailsWith<IllegalStateException> { namespace.enabled.evaluateWithTelemetry(context = context) }
        assertFailsWith<IllegalStateException> { namespace.enabled.evaluateWithTelemetryAndReason(context = context) }
        assertFailsWith<IllegalStateException> { namespace.enabled.evaluateWithAutoSpan(context = context) }
    }

    @Test
    fun `explicit telemetry instances remain isolated across namespaces`() {
        val namespaceA = TestNamespace("otel-isolation-a")
        val namespaceB = TestNamespace("otel-isolation-b")

        val probeA = telemetryProbe()
        val probeB = telemetryProbe()

        try {
            namespaceA.enabled.evaluateWithTelemetry(context = testContext("isolation-a"), telemetry = probeA.telemetry)
            namespaceB.enabled.evaluateWithTelemetry(context = testContext("isolation-b"), telemetry = probeB.telemetry)

            assertEquals(1, probeA.spanExporter.finishedSpanItems.size)
            assertEquals(1, probeB.spanExporter.finishedSpanItems.size)

            val namespaceTagA =
                probeA.spanExporter.finishedSpanItems.single().attributes.get(KonditionalSemanticAttributes.FEATURE_NAMESPACE)
            val namespaceTagB =
                probeB.spanExporter.finishedSpanItems.single().attributes.get(KonditionalSemanticAttributes.FEATURE_NAMESPACE)

            assertEquals(namespaceA.id, namespaceTagA)
            assertEquals(namespaceB.id, namespaceTagB)
        } finally {
            probeA.close()
            probeB.close()
        }
    }

    private class TestNamespace(id: String) : Namespace.TestNamespaceFacade(id) {
        val enabled by boolean<Context>(default = true)
    }

    private data class TelemetryProbe(
        val telemetry: KonditionalTelemetry,
        val spanExporter: InMemorySpanExporter,
        private val tracerProvider: SdkTracerProvider,
    ) {
        fun close() {
            tracerProvider.close()
        }
    }

    private fun telemetryProbe(): TelemetryProbe {
        val spanExporter = InMemorySpanExporter.create()
        val tracerProvider =
            SdkTracerProvider
                .builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build()
        val telemetry =
            KonditionalTelemetry(
                otel =
                    OpenTelemetrySdk
                        .builder()
                        .setTracerProvider(tracerProvider)
                        .build(),
                tracingConfig =
                    TracingConfig(
                        samplingStrategy = SamplingStrategy.ALWAYS,
                    ),
            )

        return TelemetryProbe(
            telemetry = telemetry,
            spanExporter = spanExporter,
            tracerProvider = tracerProvider,
        )
    }

    private fun testContext(stableId: String): Context =
        Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(1, 0, 0),
            stableId = StableId.of(stableId),
        )
}
