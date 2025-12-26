package io.amichne.konditional.configstate

import com.squareup.moshi.Moshi
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.SnapshotSerializer

/**
 * JSON serialization for [ConfigurationStateResponse].
 *
 * This reuses the same Moshi configuration as snapshot serialization, so custom adapters
 * (FlagValue, VersionRange, value classes) remain consistent across endpoints and storage.
 */
object ConfigurationStateSerializer {
    private val moshi: Moshi = SnapshotSerializer.defaultMoshi()
    private val responseAdapter = moshi.adapter(ConfigurationStateResponse::class.java).indent("  ")

    fun toJson(response: ConfigurationStateResponse): String = responseAdapter.toJson(response)

    fun fromJson(json: String): ParseResult<ConfigurationStateResponse> =
        try {
            val response =
                responseAdapter.fromJson(json)
                    ?: return ParseResult.Failure(ParseError.InvalidJson("Failed to parseUnsafe config-state JSON: null result"))
            ParseResult.Success(response)
        } catch (e: Exception) {
            ParseResult.Failure(ParseError.InvalidJson(e.message ?: "Unknown JSON parsing error"))
        }
}

