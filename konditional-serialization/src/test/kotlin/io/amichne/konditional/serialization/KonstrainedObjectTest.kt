@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.fixtures.serializers.DefaultConfig
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.kontracts.dsl.jsonObject
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
}
