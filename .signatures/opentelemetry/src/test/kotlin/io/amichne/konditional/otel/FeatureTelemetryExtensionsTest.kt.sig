file=opentelemetry/src/test/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensionsTest.kt
package=io.amichne.konditional.otel
imports=io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.id.StableId,io.amichne.konditional.otel.traces.KonditionalSemanticAttributes,io.amichne.konditional.otel.traces.SamplingStrategy,io.amichne.konditional.otel.traces.TracingConfig,io.opentelemetry.sdk.OpenTelemetrySdk,io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter,io.opentelemetry.sdk.trace.SdkTracerProvider,io.opentelemetry.sdk.trace.export.SimpleSpanProcessor,kotlin.test.AfterTest,kotlin.test.BeforeTest,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFailsWith
type=io.amichne.konditional.otel.FeatureTelemetryExtensionsTest|kind=class|decl=class FeatureTelemetryExtensionsTest
type=io.amichne.konditional.otel.TestNamespace|kind=class|decl=private class TestNamespace(id: String) : Namespace.TestNamespaceFacade(id)
type=io.amichne.konditional.otel.TelemetryProbe|kind=class|decl=private data class TelemetryProbe( val telemetry: KonditionalTelemetry, val spanExporter: InMemorySpanExporter, private val tracerProvider: SdkTracerProvider, )
fields:
- val enabled by boolean<Context>(default = true)
methods:
- fun setUp()
- fun tearDown()
- fun `explicit telemetry overloads work without global installation`()
- fun `deprecated global shim overloads preserve installed-global behavior`()
- fun `deprecated global shim overloads still fail when no global telemetry is installed`()
- fun `explicit telemetry instances remain isolated across namespaces`()
- private fun telemetryProbe(): TelemetryProbe
- private fun testContext(stableId: String): Context
- fun close()
