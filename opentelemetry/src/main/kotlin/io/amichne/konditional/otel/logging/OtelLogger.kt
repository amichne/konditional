package io.amichne.konditional.otel.logging

import io.amichne.konditional.core.ops.KonditionalLogger
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.Context as OtelContext

/**
 * OpenTelemetry implementation of [KonditionalLogger].
 *
 * Emits structured log records with trace context propagation. Log records are automatically
 * correlated with active spans via OpenTelemetry context.
 *
 * @property logger OpenTelemetry logger instance.
 */
class OtelLogger(
    private val logger: Logger,
) : KonditionalLogger {
    override fun debug(message: () -> String) {
        emitLog(Severity.DEBUG, message(), throwable = null)
    }

    override fun info(message: () -> String) {
        emitLog(Severity.INFO, message(), throwable = null)
    }

    override fun warn(
        message: () -> String,
        throwable: Throwable?,
    ) {
        emitLog(Severity.WARN, message(), throwable)
    }

    override fun error(
        message: () -> String,
        throwable: Throwable?,
    ) {
        emitLog(Severity.ERROR, message(), throwable)
    }

    private fun emitLog(
        severity: Severity,
        message: String,
        throwable: Throwable?,
    ) {
        val builder =
            logger
                .logRecordBuilder()
                .setContext(OtelContext.current())
                .setSeverity(severity)
                .setBody(message)
                .setAttribute(AttributeKey.stringKey("logger.name"), "io.amichne.konditional")

        throwable?.let {
            builder.setAttribute(AttributeKey.stringKey("exception.type"), it::class.qualifiedName ?: "unknown")
            builder.setAttribute(AttributeKey.stringKey("exception.message"), it.message ?: "")
            it.stackTraceToString().takeIf { stack -> stack.length <= 1000 }?.let { stack ->
                builder.setAttribute(AttributeKey.stringKey("exception.stacktrace"), stack)
            }
        }

        builder.emit()
    }
}
