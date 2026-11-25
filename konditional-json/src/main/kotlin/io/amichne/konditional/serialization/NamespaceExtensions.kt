package io.amichne.konditional.serialization

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.toParseResult
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec

/**
 * Exports the namespace configuration.
 * @throws KonditionalBoundaryFailure if a rule uses a code-only predicate or contextual value resolver.
 */
fun Namespace<*>.toJson(): String = ConfigurationCodec.encode(this)

/**
 * Loads a complete snapshot after checking each value against its declared feature.
 * Resolves every fact reference and operand against this namespace before installation.
 * A failed decode leaves the active configuration intact.
 */
fun Namespace<*>.fromJson(json: String): ParseResult<Configuration> =
    ConfigurationCodec.decode(json, this)
        .onSuccess { configuration -> load(configuration) }
        .toParseResult()
