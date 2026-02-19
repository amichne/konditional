file=konditional-otel/src/main/kotlin/io/amichne/konditional/otel/logging/OtelLogger.kt
package=io.amichne.konditional.otel.logging
imports=io.amichne.konditional.core.ops.KonditionalLogger,io.opentelemetry.api.common.AttributeKey,io.opentelemetry.api.logs.Logger,io.opentelemetry.api.logs.Severity,io.opentelemetry.context.Context
type=io.amichne.konditional.otel.logging.OtelLogger|kind=class|decl=class OtelLogger( private val logger: Logger, ) : KonditionalLogger
methods:
- override fun debug(message: () -> String)
- override fun info(message: () -> String)
- override fun warn( message: () -> String, throwable: Throwable?, )
- override fun error( message: () -> String, throwable: Throwable?, )
