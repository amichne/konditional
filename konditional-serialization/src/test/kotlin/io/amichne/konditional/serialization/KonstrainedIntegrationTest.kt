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
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Comprehensive integration tests for Konstrained<ObjectSchema> support.
 *
 * These tests validate:
 * - Data class to JsonValue conversion
 * - JsonValue to data class parsing
 * - Schema generation and validation
 * - Serialization with Moshi
 * - Feature flag integration
 */
class KonstrainedIntegrationTest {

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
        val json = jsonObject {
            field("theme") { string("dark") }
            field("notificationsEnabled") { boolean(false) }
            field("maxRetries") { number(7.0) }
            field("timeout") { number(120.0) }
        }
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

        val invalidJson = jsonObject {
            field("theme") { string("") } // too short
            field("notificationsEnabled") { boolean(true) }
            field("maxRetries") { number(-1.0) } // below min
            field("timeout") { number(500.0) } // above max
        }
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
}
