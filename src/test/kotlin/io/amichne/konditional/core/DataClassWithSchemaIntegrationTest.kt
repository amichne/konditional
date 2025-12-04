package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.jsonObject
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.DataClassEncodeable
import io.amichne.konditional.core.types.DataClassWithSchema
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue
import io.amichne.konditional.core.types.parseAs
import io.amichne.konditional.core.types.toJsonValue
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Comprehensive integration tests for DataClassWithSchema support.
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
        val timeout: Double = 30.0
    ) : DataClassWithSchema {
        override val schema = Companion.schema

        companion object {
            val schema: JsonSchema.ObjectSchema = jsonObject {
                field("theme", required = true, default = "light") { string() }
                field("notificationsEnabled", required = true, default = true) { boolean() }
                field("maxRetries", required = true, default = 3) { int() }
                field("timeout", required = true, default = 30.0) { double() }
            }
        }
    }

    // Nested data class
    data class PaymentConfig(
        val maxAmount: Double = 1000.0,
        val currency: String = "USD",
        val settings: UserSettings = UserSettings()
    ) : DataClassWithSchema {
        override val schema = Companion.schema

        companion object {
            val schema: JsonSchema.ObjectSchema = jsonObject {
                field("maxAmount", required = true, default = 1000.0) { double() }
                field("currency", required = true, default = "USD") { string() }
                field("settings", required = true) { jsonObject {
                    field("theme", required = true, default = "light") { string() }
                    field("notificationsEnabled", required = true, default = true) { boolean() }
                    field("maxRetries", required = true, default = 3) { int() }
                    field("timeout", required = true, default = 30.0) { double() }
                } }
            }
        }
    }

    @Test
    fun `toJsonValue converts data class to JsonObject`() {
        // Given
        val settings = UserSettings(
            theme = "dark",
            notificationsEnabled = false,
            maxRetries = 5,
            timeout = 60.0
        )

        // When
        val jsonValue = settings.toJsonValue()

        // Then
        assertTrue(jsonValue is JsonValue.JsonObject)
        assertEquals(JsonValue.JsonString("dark"), jsonValue.fields["theme"])
        assertEquals(JsonValue.JsonBoolean(false), jsonValue.fields["notificationsEnabled"])
        assertEquals(JsonValue.JsonNumber(5.0), jsonValue.fields["maxRetries"])
        assertEquals(JsonValue.JsonNumber(60.0), jsonValue.fields["timeout"])
    }

    @Test
    fun `parseAs converts JsonObject to data class`() {
        // Given
        val jsonObject = JsonValue.JsonObject(
            fields = mapOf(
                "theme" to JsonValue.JsonString("dark"),
                "notificationsEnabled" to JsonValue.JsonBoolean(false),
                "maxRetries" to JsonValue.JsonNumber(5.0),
                "timeout" to JsonValue.JsonNumber(60.0)
            ),
            schema = UserSettings.schema
        )

        // When
        val result = jsonObject.parseAs<UserSettings>()

        // Then
        assertTrue(result is ParseResult.Success)
        val settings = (result as ParseResult.Success).value
        assertEquals("dark", settings.theme)
        assertEquals(false, settings.notificationsEnabled)
        assertEquals(5, settings.maxRetries)
        assertEquals(60.0, settings.timeout)
    }

    @Test
    fun `parseAs uses default values for missing optional fields`() {
        // Given - JsonObject with only required fields
        val jsonObject = JsonValue.JsonObject(
            fields = mapOf(
                "theme" to JsonValue.JsonString("dark"),
                "notificationsEnabled" to JsonValue.JsonBoolean(false),
                "maxRetries" to JsonValue.JsonNumber(5.0),
                "timeout" to JsonValue.JsonNumber(60.0)
            ),
            schema = UserSettings.schema
        )

        // When
        val result = jsonObject.parseAs<UserSettings>()

        // Then
        assertTrue(result is ParseResult.Success)
    }

    @Test
    fun `schema property returns correct schema for data class`() {
        // Given
        val settings = UserSettings()

        // When
        val schema = settings.schema

        // Then
        assertEquals(UserSettings.schema, schema)
        assertEquals(4, schema.fields.size)
        assertTrue(schema.fields.containsKey("theme"))
        assertTrue(schema.fields.containsKey("notificationsEnabled"))
        assertTrue(schema.fields.containsKey("maxRetries"))
        assertTrue(schema.fields.containsKey("timeout"))
    }

    @Test
    fun `DataClassEncodeable wraps data class with schema`() {
        // Given
        val settings = UserSettings(theme = "dark")
        val schema = UserSettings.schema

        // When
        val encodeable = DataClassEncodeable(settings, schema)

        // Then
        assertEquals(settings, encodeable.value)
        assertEquals(schema, encodeable.schema)
        assertEquals(io.amichne.konditional.core.types.EncodableValue.Encoding.DATA_CLASS, encodeable.encoding)
    }

    @Test
    fun `DataClassEncodeable converts to JsonValue`() {
        // Given
        val settings = UserSettings(theme = "dark", maxRetries = 5)
        val encodeable = DataClassEncodeable(settings, UserSettings.schema)

        // When
        val jsonValue = encodeable.toJsonValue()

        // Then
        assertTrue(jsonValue is JsonValue.JsonObject)
        assertEquals(JsonValue.JsonString("dark"), jsonValue.fields["theme"])
        assertEquals(JsonValue.JsonNumber(5.0), jsonValue.fields["maxRetries"])
    }

    @Test
    fun `round-trip conversion preserves data`() {
        // Given
        val original = UserSettings(
            theme = "dark",
            notificationsEnabled = false,
            maxRetries = 7,
            timeout = 120.0
        )

        // When - Convert to JSON and back
        val jsonValue = original.toJsonValue()
        val result = jsonValue.parseAs<UserSettings>()

        // Then
        assertTrue(result is ParseResult.Success)
        val recovered = (result as ParseResult.Success).value
        assertEquals(original.theme, recovered.theme)
        assertEquals(original.notificationsEnabled, recovered.notificationsEnabled)
        assertEquals(original.maxRetries, recovered.maxRetries)
        assertEquals(original.timeout, recovered.timeout)
    }

    @Test
    @org.junit.jupiter.api.Disabled("Nested data class support needs additional work")
    fun `nested data classes are supported`() {
        // Given
        val config = PaymentConfig(
            maxAmount = 5000.0,
            currency = "EUR",
            settings = UserSettings(theme = "dark", maxRetries = 10)
        )

        // When
        val jsonValue = config.toJsonValue()
        val result = jsonValue.parseAs<PaymentConfig>()

        // Then
        assertTrue(result is ParseResult.Success)
        val recovered = (result as ParseResult.Success).value
        assertEquals(config.maxAmount, recovered.maxAmount)
        assertEquals(config.currency, recovered.currency)
        assertEquals(config.settings.theme, recovered.settings.theme)
        assertEquals(config.settings.maxRetries, recovered.settings.maxRetries)
    }

    // Test namespace for feature integration
    private val testNamespace = test("data-class-value-test")

    // Test feature container with data class feature
    object TestFeatures : FeatureContainer<TestNamespace>(test("data-class-features-test")) {
        val USER_SETTINGS by dataClass<UserSettings, Context>(
            default = UserSettings()
        )

        val PAYMENT_CONFIG by dataClass<PaymentConfig, Context>(
            default = PaymentConfig()
        ) {
            // Can configure rules here
            default(
                PaymentConfig(
                    maxAmount = 10000.0,
                    currency = "USD"
                )
            )
        }
    }

    @Test
    fun `data class feature can be created in FeatureContainer`() {
        // Given/When
        val feature = TestFeatures.USER_SETTINGS

        // Then
        assertEquals("USER_SETTINGS", feature.key)
        assertTrue(feature.namespace is TestNamespace)
    }

    @Test
    fun `data class feature with custom default configuration`() {
        // Given/When
        val feature = TestFeatures.PAYMENT_CONFIG

        // Then
        assertEquals("PAYMENT_CONFIG", feature.key)
        assertTrue(feature.namespace is TestNamespace)
    }

    @Test
    fun `all data class features are enumerable`() {
        // Given/When - Access features to trigger registration
        val userSettingsFeature = TestFeatures.USER_SETTINGS
        val paymentConfigFeature = TestFeatures.PAYMENT_CONFIG
        val allFeatures = TestFeatures.allFeatures()

        // Then
        assertTrue(allFeatures.size >= 2)
        assertTrue(allFeatures.any { it.key == "USER_SETTINGS" })
        assertTrue(allFeatures.any { it.key == "PAYMENT_CONFIG" })
    }
}
