package io.amichne.konditional.serialization

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.toParseResult
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec

/**
 * Exports the namespace configuration.
 * @throws KonditionalBoundaryFailure if the snapshot cannot preserve a rule's targeting conditions.
 */
fun Namespace.toJson(): String = ConfigurationCodec.encode(this)

/**
 * Loads a complete snapshot after checking each value against its declared feature.
 * A contextual rule has no restorable resolver in JSON and yields [io.amichne.konditional.core.result.ParseError.InvalidSnapshot].
 * A failed decode leaves the active configuration intact.
 */
fun Namespace.fromJson(json: String): ParseResult<Configuration> =
    ConfigurationCodec.decode(json, this)
        .onSuccess { configuration -> load(configuration) }
        .toParseResult()
