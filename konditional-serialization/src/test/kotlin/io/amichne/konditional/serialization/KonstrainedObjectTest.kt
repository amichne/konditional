@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.fixtures.serializers.DefaultConfig
import io.amichne.konditional.fixtures.serializers.Email
import io.amichne.konditional.fixtures.serializers.FeatureEnabled
import io.amichne.konditional.fixtures.serializers.Percentage
import io.amichne.konditional.fixtures.serializers.RetryCount
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.serializers.Tags
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.serialization.instance.ConfigValue
import io.amichne.kontracts.dsl.intSchema
import io.amichne.kontracts.dsl.jsonArray
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.dsl.jsonValue
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.dsl.stringSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A minimal data class with NO Kotlin default values, used to exercise the
 * ParseError path when a required field is absent from the JSON payload.
 *
 * Because all fields are non-nullable and have no default, [param.isOptional] is
 * false for every constructor parameter. Omitting any field from the JSON must
 * produce a [io.amichne.konditional.core.result.ParseError.InvalidSnapshot].
 *
 * The schema uses [required] entries so that [resolveSchemaParameter] sees
 * [FieldSchema.required] == true when a field is absent from JSON.
 */
private data class StrictConfig(
    val id: String,
    val count: Int,
) : Konstrained.Object<ObjectSchema> {
    override val schema: ObjectSchema =
        schema {
            required("id", stringSchema())
            required("count", intSchema())
        }
}

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
        val expected = RetryPolicy(maxAttempts = 5, backoffMs = 500.0, enabled = true, mode = "linear")
        assertEquals(expected, result.getOrThrow())
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

    @Test
    fun `decodeKonstrained dispatches JsonArray to decodeKonstrainedPrimitive for Tags`() {
        val json = jsonArray {
            elements(listOf(jsonValue { string("alpha") }, jsonValue { string("beta") }))
        }
        val result = SchemaValueCodec.decodeKonstrained(Tags::class, json)
        assertTrue(result.isSuccess)
        assertEquals(Tags(listOf("alpha", "beta")), result.getOrThrow())
    }

    // =========================================================================
    // encode → decode roundtrip (data class)
    // =========================================================================

    @Test
    fun `encodeKonstrained then decodeKonstrained round-trips RetryPolicy`() {
        val rp = RetryPolicy(maxAttempts = 3, backoffMs = 200.0, enabled = false, mode = "exponential")
        val json = SchemaValueCodec.encodeKonstrained(rp)
        assertTrue(json is JsonObject, "encodeKonstrained must produce a JsonObject for a data class")
        val result = SchemaValueCodec.decodeKonstrained(RetryPolicy::class, json)
        assertTrue(result.isSuccess)
        assertEquals(rp, result.getOrThrow())
    }

    // =========================================================================
    // schema defaultValue and optional fields
    // =========================================================================

    @Test
    fun `decode uses schema defaultValue when field absent from JSON`() {
        // UserSettings.notificationsEnabled has `default = true` in its schema definition.
        // Omit that field from the JSON so the codec falls back to the schema defaultValue.
        val json = jsonObject {
            field("theme") { string("dark") }
            field("maxRetries") { number(5.0) }
            field("timeout") { number(60.0) }
        }
        val instance = UserSettings(theme = "dark", maxRetries = 5, timeout = 60.0)
        val result = SchemaValueCodec.decode(UserSettings::class, json, instance.schema)
        assertTrue(result.isSuccess)
        // The schema default for notificationsEnabled is `true`; it must be used here.
        assertEquals(true, result.getOrThrow().notificationsEnabled)
    }

    @Test
    fun `decode skips optional field absent from JSON and uses Kotlin default`() {
        // RetryPolicy.mode has a Kotlin default of "exponential". Omitting it from JSON
        // causes resolveSchemaParameter to reach the `param.isOptional` branch (Skip),
        // letting Kotlin supply the default value via callBy.
        val json = jsonObject {
            field("maxAttempts") { number(7.0) }
            field("backoffMs") { number(500.0) }
            field("enabled") { boolean(false) }
        }
        val result = SchemaValueCodec.decodeKonstrained(RetryPolicy::class, json)
        assertTrue(result.isSuccess)
        assertEquals("exponential", result.getOrThrow().mode)
    }

    @Test
    fun `decode fails with ParseError when required field is absent`() {
        // StrictConfig has no Kotlin default values, so param.isOptional is false for
        // every constructor parameter. The schema marks all fields required (non-nullable).
        // Omitting `count` from the JSON payload must reach the ParseError branch inside
        // resolveSchemaParameter (required == true, no schema defaultValue, not optional).
        val json = jsonObject {
            field("id") { string("abc-123") }
            // `count` intentionally omitted
        }
        val strictConfig = StrictConfig(id = "abc-123", count = 0)
        val result = SchemaValueCodec.decode(StrictConfig::class, json, strictConfig.schema)
        assertFalse(result.isSuccess, "decode must produce a ParseError when a required field is absent")
    }

    // =========================================================================
    // FlagValue and ConfigValue roundtrip (data class)
    // =========================================================================

    @Test
    fun `FlagValue from RetryPolicy produces DataClassValue`() {
        val rp = RetryPolicy(maxAttempts = 3, backoffMs = 200.0, enabled = false, mode = "exponential")
        val fv = FlagValue.from(rp)
        assertInstanceOf(FlagValue.DataClassValue::class.java, fv)
        assertEquals(RetryPolicy::class.java.name, (fv as FlagValue.DataClassValue).dataClassName)
    }

    @Test
    fun `FlagValue extractValue round-trips RetryPolicy`() {
        val rp = RetryPolicy(maxAttempts = 3, backoffMs = 200.0, enabled = false, mode = "exponential")
        val fv = FlagValue.from(rp) as FlagValue.DataClassValue
        val decoded = fv.extractValue<RetryPolicy>(expectedSample = rp)
        assertEquals(rp, decoded)
    }

    @Test
    fun `ConfigValue from RetryPolicy produces DataClassValue`() {
        val rp = RetryPolicy(maxAttempts = 3, backoffMs = 200.0, enabled = false, mode = "exponential")
        val cv = ConfigValue.from(rp)
        assertInstanceOf(ConfigValue.DataClassValue::class.java, cv)
        assertEquals(RetryPolicy::class.java.name, (cv as ConfigValue.DataClassValue).dataClassName)
    }
}
