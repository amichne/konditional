@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import com.squareup.moshi.Moshi
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.utils.onSuccess
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.kontracts.dsl.reflectiveSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ReflectiveObjectSchema
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Comprehensive integration tests for Konstrained schema support.
 *
 * These tests validate:
 * - Data class to JsonValue conversion
 * - JsonValue to data class parsing
 * - Schema generation and validation
 * - Serialization with Moshi
 * - Feature flag integration
 */
class KonstrainedIntegrationTest {
    data class ReflectiveSettings(
        val theme: String = "light",
        val maxRetries: Int = 3,
        val enabled: Boolean = true,
    ) : Konstrained<ReflectiveObjectSchema<ReflectiveSettings>> {
        override val schema: ReflectiveObjectSchema<ReflectiveSettings> =
            reflectiveSchema<ReflectiveSettings> {
                required(
                    property = ReflectiveSettings::theme,
                    schema = JsonSchema.string(minLength = 1, enum = listOf("light", "dark", "auto")),
                )
                required(
                    property = ReflectiveSettings::maxRetries,
                    schema = JsonSchema.int(minimum = 0, maximum = 10),
                )
                optional(
                    property = ReflectiveSettings::enabled,
                    schema = JsonSchema.boolean(default = true),
                )
            }
    }

    @Test
    fun `data class to JsonValue conversion is correct`() {
        val settings = UserSettings()
        val json = SchemaValueCodec.encode(settings, settings.schema)
        val fields = json.fields
        assertEquals("light", fields["theme"]?.let { (it as? JsonString)?.value })
        assertEquals(true, fields["notificationsEnabled"]?.let { (it as? JsonBoolean)?.value })
        assertEquals(3.0, fields["maxRetries"]?.let { (it as? JsonNumber)?.value })
        assertEquals(30.0, fields["timeout"]?.let { (it as? JsonNumber)?.value })
    }

    @Test
    fun `JsonValue to data class parsing is correct`() {
        val json = JsonObject(
            fields = mapOf(
                "theme" to JsonString("dark"),
                "notificationsEnabled" to JsonBoolean(false),
                "maxRetries" to JsonNumber(7.0),
                "timeout" to JsonNumber(120.0)
            ),
            schema = null
        )
        val result = SchemaValueCodec.decode(UserSettings::class, json, UserSettings().schema)
        assertTrue(result is ParseResult.Success)
        val settings = (result as ParseResult.Success).value
        assertEquals("dark", settings.theme)
        assertEquals(false, settings.notificationsEnabled)
        assertEquals(7, settings.maxRetries)
        assertEquals(120.0, settings.timeout)
    }

    @Test
    fun `schema generation and validation works as expected`() {
        val settings = UserSettings()
        val schema = settings.schema
        val validJson = SchemaValueCodec.encode(settings, schema)
        val validResult = validJson.validate(schema)
        assertTrue(validResult.isValid)

        val invalidJson = JsonObject(
            fields = mapOf(
                "theme" to JsonString(""), // too short
                "notificationsEnabled" to JsonBoolean(true),
                "maxRetries" to JsonNumber(-1.0), // below min
                "timeout" to JsonNumber(500.0) // above max
            ),
            schema = null
        )
        val invalidResult = invalidJson.validate(schema)
        assertTrue(invalidResult.isInvalid)
        assertNotNull(invalidResult.getErrorMessage())
    }

    @Test
    fun `Moshi serialization and deserialization roundtrip works`() {
        val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(UserSettings::class.java)
        val original = UserSettings(theme = "auto", notificationsEnabled = false, maxRetries = 5, timeout = 42.5)
        val json = adapter.toJson(original).also { println(it) }
        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `feature flag integration with data class works`() {
        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version(1, 0, 0),
            stableId = TestStableId
        )
        val features = object : Namespace.TestNamespaceFacade("data-class-flag") {
            val userSettings by custom<UserSettings, Context>(default = UserSettings()) {}
        }
        // Default value
        assertEquals(UserSettings(), features.userSettings.evaluate(context))
        // Override
        val override = UserSettings(theme = "dark", notificationsEnabled = false, maxRetries = 1, timeout = 10.0)
        features.withOverride(features.userSettings, override) {
            assertEquals(override, features.userSettings.evaluate(context))
            val json = ConfigurationSnapshotCodec.encode(features.configuration)
            println(json)
            // Verify round-trip serialization works
            ConfigurationSnapshotCodec.decode(json).onSuccess { config ->
                println("Successfully deserialized ${config.flags.size} flags")
            }
        }
    }

    @Test
    fun `SchemaValueCodec supports reflective object schemas via object conversion`() {
        val original = ReflectiveSettings(theme = "dark", maxRetries = 5, enabled = false)
        val schema = original.schema.asObjectSchema()
        val encoded = SchemaValueCodec.encode(original, schema)
        val decoded = SchemaValueCodec.decode(ReflectiveSettings::class, encoded, schema)
        assertTrue(decoded is ParseResult.Success)
        assertEquals(original, (decoded as ParseResult.Success).value)
    }

    @Test
    fun `FlagValue roundtrip works with reflective schema based data classes`() {
        val original = ReflectiveSettings(theme = "auto", maxRetries = 4, enabled = true)
        val serialized = FlagValue.from(original)
        assertTrue(serialized is FlagValue.DataClassValue)
        val reconstructed = serialized.extractValue<ReflectiveSettings>(original.schema.asObjectSchema())
        assertEquals(original, reconstructed)
    }
}
