@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.fixtures.serializers.DefaultConfig
import io.amichne.konditional.fixtures.serializers.Email
import io.amichne.konditional.fixtures.serializers.FeatureEnabled
import io.amichne.konditional.fixtures.serializers.Percentage
import io.amichne.konditional.fixtures.serializers.RetryCount
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.dsl.jsonValue
import io.amichne.kontracts.value.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for Konstrained.Object encoding/decoding:
 * - Kotlin `object` singleton round-trip (the fix target)
 * - `data class` encode → decode roundtrip via [SchemaValueCodec.decode]
 * - [SchemaValueCodec.encodeKonstrained] dispatch through ObjectTraits
 * - Default-value and optional-field behaviour during decode
 */
class KonstrainedObjectTest {

    // =========================================================================
    // Kotlin `object` singleton
    // =========================================================================

    @Test
    fun `decode returns objectInstance for Kotlin object singleton with schema`() {
        val emptyJson = jsonObject {}
        val result = SchemaValueCodec.decode(DefaultConfig::class, emptyJson, DefaultConfig.schema)
        assertTrue(result.isSuccess)
        assertSame(DefaultConfig, result.getOrThrow())
    }

    @Test
    fun `decode returns objectInstance for Kotlin object singleton without schema`() {
        val emptyJson = jsonObject {}
        val result = SchemaValueCodec.decode(DefaultConfig::class, emptyJson)
        assertTrue(result.isSuccess)
        assertSame(DefaultConfig, result.getOrThrow())
    }

    @Test
    fun `encodeKonstrained dispatches Konstrained Object singleton through ObjectTraits`() {
        val encoded = SchemaValueCodec.encodeKonstrained(DefaultConfig)
        assertTrue(encoded is JsonObject)
        assertEquals(0, (encoded as JsonObject).fields.size)
    }

    // =========================================================================
    // decodeKonstrained — unified dispatch
    // =========================================================================

    @Test
    fun `decodeKonstrained dispatches JsonObject to decode for data class`() {
        val json = jsonObject {
            field("maxAttempts") { number(5.0) }
            field("backoffMs") { number(500.0) }
            field("enabled") { boolean(true) }
            field("mode") { string("linear") }
        }
        val result = SchemaValueCodec.decodeKonstrained(RetryPolicy::class, json)
        assertTrue(result.isSuccess)
        assertEquals(RetryPolicy(maxAttempts = 5, backoffMs = 500.0, enabled = true, mode = "linear"), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrained dispatches JsonObject to objectInstance for singleton`() {
        val result = SchemaValueCodec.decodeKonstrained(DefaultConfig::class, jsonObject {})
        assertTrue(result.isSuccess)
        assertSame(DefaultConfig, result.getOrThrow())
    }

    @Test
    fun `decodeKonstrained dispatches JsonString to decodeKonstrainedPrimitive for Email`() {
        val result = SchemaValueCodec.decodeKonstrained(Email::class, jsonValue { string("test@example.com") })
        assertTrue(result.isSuccess)
        assertEquals(Email("test@example.com"), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrained dispatches JsonBoolean to decodeKonstrainedPrimitive for FeatureEnabled`() {
        val result = SchemaValueCodec.decodeKonstrained(FeatureEnabled::class, jsonValue { boolean(true) })
        assertTrue(result.isSuccess)
        assertEquals(FeatureEnabled(true), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrained dispatches JsonNumber to decodeKonstrainedPrimitive for RetryCount (Int)`() {
        val result = SchemaValueCodec.decodeKonstrained(RetryCount::class, jsonValue { number(3) })
        assertTrue(result.isSuccess)
        assertEquals(RetryCount(3), result.getOrThrow())
    }

    @Test
    fun `decodeKonstrained dispatches JsonNumber to decodeKonstrainedPrimitive for Percentage (Double)`() {
        val result = SchemaValueCodec.decodeKonstrained(Percentage::class, jsonValue { number(75.5) })
        assertTrue(result.isSuccess)
        assertEquals(Percentage(75.5), result.getOrThrow())
    }
}
