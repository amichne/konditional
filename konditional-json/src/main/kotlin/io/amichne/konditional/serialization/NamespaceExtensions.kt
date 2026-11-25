package io.amichne.konditional.serialization

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.toParseResult
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec

/**
 * Writes the namespace configuration. Throws [KonditionalBoundaryFailure] when a rule uses
 * a code-only targeting function or pairs a contextual value with expression-only targeting.
 */
fun Namespace.toJson(): String = ConfigurationCodec.encode(this)

/** Returns a typed failure when the snapshot format cannot preserve a rule's targeting. */
fun Namespace.tryToJson(): ParseResult<String> =
    try {
        ParseResult.Success(toJson())
    } catch (failure: KonditionalBoundaryFailure) {
        ParseResult.Failure(failure.parseError)
    }

/**
 * Loads a complete snapshot after checking each value against its declared feature.
 * A contextual rule has no restorable resolver in JSON and yields [io.amichne.konditional.core.result.ParseError.InvalidSnapshot].
 * A failed decode leaves the active configuration intact.
 */
fun Namespace.fromJson(json: String): ParseResult<Configuration> =
    ConfigurationCodec.decode(json, this)
        .onSuccess { configuration -> load(configuration) }
        .toParseResult()
