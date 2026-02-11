file=opentelemetry/src/test/kotlin/io/amichne/konditional/otel/SimpleIntegrationTest.kt
package=io.amichne.konditional.otel
imports=io.amichne.konditional.otel.traces.SamplingStrategy,io.amichne.konditional.otel.traces.TracingConfig,io.opentelemetry.sdk.OpenTelemetrySdk,io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter,io.opentelemetry.sdk.trace.SdkTracerProvider,io.opentelemetry.sdk.trace.export.SimpleSpanProcessor,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertNotNull
type=io.amichne.konditional.otel.SimpleIntegrationTest|kind=class|decl=class SimpleIntegrationTest
methods:
- fun `can create telemetry instance`()
- fun `noop telemetry works`()
- fun `global instance can be installed and retrieved`()
- fun `toRegistryHooks creates valid hooks`()
- fun `TracingConfig can be configured`()
- fun `RATIO sampling validates percentage`()
- fun `telemetry integrates with OpenTelemetry SDK`()
