package io.amichne.konditional.core

import com.squareup.moshi.Moshi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.JsonSchemaClass
import io.amichne.konditional.core.types.parseAs
import io.amichne.konditional.core.types.toJsonValue
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.fixtures.core.withOverride
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Comprehensive integration tests for JsonSchemaClass support.
 *
 * These tests validate:
 * - Data class to JsonValue conversion
 * - JsonValue to data class parsing
 * - Schema generation and validation
 * - Serialization with Moshi
 * - Feature flag integration
 */
class DataClassWithSchemaIntegrationTest {
    // Test data class with manually defined schema
    data class UserSettings(
        val theme: String = "light",
        val notificationsEnabled: Boolean = true,
        val maxRetries: Int = 3,
        val timeout: Double = 30.0,
    ) : JsonSchemaClass {
        override val schema = schemaRoot {
            // Type-inferred DSL: no need to call string(), the property type determines the schema
            ::theme of {
                minLength = 1
                maxLength = 50
                description = "UI theme preference"
                enum = listOf("light", "dark", "auto")
            }

            // Boolean schema with automatic type inference
            ::notificationsEnabled of {
                description = "Enable push notifications"
                default = true
            }

            // Int schema with constraints
            ::maxRetries of {
                minimum = 0
                maximum = 10
                description = "Maximum retry attempts"
            }

            // Double schema with format
            ::timeout of {
                minimum = 0.0
                maximum = 300.0
                format = "double"
                description = "Request timeout in seconds"
            }

        }
    }

    @Test
    fun `data class to JsonValue conversion is correct`() {
        val settings = UserSettings()
        val json = settings.toJsonValue()
        val fields = json.fields
        assertEquals("light", fields["theme"]?.let { (it as? JsonString)?.value })
        assertEquals(true, fields["notificationsEnabled"]?.let { (it as? JsonBoolean)?.value })
        assertEquals(3.0, fields["maxRetries"]?.let { (it as? JsonNumber)?.value })
        assertEquals(30.0, fields["timeout"]?.let { (it as? JsonNumber)?.value })
    }

    @Test
    fun `JsonValue to data class parsing is correct`() {
        val json = JsonObject(
            mapOf(
                "theme" to JsonString("dark"),
                "notificationsEnabled" to JsonBoolean(false),
                "maxRetries" to JsonNumber(7.0),
                "timeout" to JsonNumber(120.0)
            )
        )
        val result = json.parseAs<UserSettings>()
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
        val validJson = settings.toJsonValue()
        val validResult = validJson.validate(schema)
        assertTrue(validResult.isValid)

        val invalidJson = JsonObject(
            mapOf(
                "theme" to JsonString(""), // too short
                "notificationsEnabled" to JsonBoolean(true),
                "maxRetries" to JsonNumber(-1.0), // below min
                "timeout" to JsonNumber(500.0) // above max
            )
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
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `feature flag integration with data class works`() {
        val testNamespace = TestNamespace.test("data-class-flag")
        val context = Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.WEB,
            appVersion = Version(1, 0, 0),
            stableId = TestStableId
        )
        val Features = object : FeatureContainer<TestNamespace>(testNamespace) {
            val userSettings by dataClass<UserSettings, Context>(default = UserSettings()) {}
        }
        // Default value
        assertEquals(UserSettings(), context.evaluate(Features.userSettings))
        // Override
        val override = UserSettings(theme = "dark", notificationsEnabled = false, maxRetries = 1, timeout = 10.0)
        testNamespace.withOverride(Features.userSettings, override) {
            assertEquals(override, context.evaluate(Features.userSettings))
        }
    }
}
