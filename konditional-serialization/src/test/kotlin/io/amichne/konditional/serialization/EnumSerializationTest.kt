@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.ValueType
import io.amichne.konditional.internal.serialization.adapters.FlagValueAdapterFactory
import io.amichne.konditional.internal.serialization.models.FlagValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Test suite for enum value serialization and deserialization
 */
class EnumSerializationTest {

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    enum class Theme {
        LIGHT, DARK, AUTO
    }

    private val moshi = Moshi.Builder()
        .add(FlagValueAdapterFactory)
        .build()

    private val adapter = moshi.adapter(FlagValue::class.java)

    @Test
    fun `FlagValue from creates EnumValue for enum types`() {
        val logLevel = LogLevel.INFO
        val flagValue = FlagValue.from(logLevel)

        assert(flagValue is FlagValue.EnumValue)
        flagValue as FlagValue.EnumValue
        assertEquals("INFO", flagValue.value)
        assertEquals(LogLevel::class.java.name, flagValue.enumClassName)
    }

    @Test
    fun `FlagValue from handles all enum values`() {
        LogLevel.entries.forEach { level ->
            val flagValue = FlagValue.from(level)
            assert(flagValue is FlagValue.EnumValue)
            flagValue as FlagValue.EnumValue
            assertEquals(level.name, flagValue.value)
        }
    }

    @Test
    fun `EnumValue toValueType returns ENUM`() {
        val enumValue = FlagValue.EnumValue("INFO", LogLevel::class.java.name)
        assertEquals(ValueType.ENUM, enumValue.toValueType())
    }

    @Test
    fun `EnumValue serializes to JSON correctly`() {
        val enumValue = FlagValue.EnumValue("DEBUG", LogLevel::class.java.name)
        val json = adapter.toJson(enumValue)

        // Should include type, value, and enumClassName
        assert(json.contains("\"type\":\"ENUM\""))
        assert(json.contains("\"value\":\"DEBUG\""))
        assert(json.contains("\"enumClassName\":\"${LogLevel::class.java.name}\""))
    }

    @Test
    fun `EnumValue deserializes from JSON correctly`() {
        val json = """
            {
                "type": "ENUM",
                "value": "WARN",
                "enumClassName": "${LogLevel::class.java.name}"
            }
        """.trimIndent()

        val flagValue = adapter.fromJson(json)

        assert(flagValue is FlagValue.EnumValue)
        flagValue as FlagValue.EnumValue
        assertEquals("WARN", flagValue.value)
        assertEquals(LogLevel::class.java.name, flagValue.enumClassName)
    }

    @Test
    fun `EnumValue round-trip serialization preserves value`() {
        val original = FlagValue.EnumValue("ERROR", LogLevel::class.java.name)
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)

        assert(deserialized is FlagValue.EnumValue)
        deserialized as FlagValue.EnumValue
        assertEquals(original.value, deserialized.value)
        assertEquals(original.enumClassName, deserialized.enumClassName)
    }

    @Test
    fun `EnumValue deserialization fails without value`() {
        val json = """
            {
                "type": "ENUM",
                "enumClassName": "${LogLevel::class.java.name}"
            }
        """.trimIndent()

        assertThrows(JsonDataException::class.java) {
            adapter.fromJson(json)
        }
    }

    @Test
    fun `EnumValue deserialization fails without enumClassName`() {
        val json = """
            {
                "type": "ENUM",
                "value": "INFO"
            }
        """.trimIndent()

        assertThrows(JsonDataException::class.java) {
            adapter.fromJson(json)
        }
    }

    @Test
    fun `EnumValue works with different enum types`() {
        val logLevel = FlagValue.from(LogLevel.DEBUG)
        val theme = FlagValue.from(Theme.DARK)

        assert(logLevel is FlagValue.EnumValue)
        assert(theme is FlagValue.EnumValue)

        logLevel as FlagValue.EnumValue
        theme as FlagValue.EnumValue

        assertEquals("DEBUG", logLevel.value)
        assertEquals(LogLevel::class.java.name, logLevel.enumClassName)

        assertEquals("DARK", theme.value)
        assertEquals(Theme::class.java.name, theme.enumClassName)
    }

    @Test
    fun `EnumValue JSON distinguishes between different enum types`() {
        val logLevelValue = FlagValue.EnumValue("INFO", LogLevel::class.java.name)
        val themeValue = FlagValue.EnumValue("DARK", Theme::class.java.name)

        val logLevelJson = adapter.toJson(logLevelValue)
        val themeJson = adapter.toJson(themeValue)

        // JSON should contain different class names
        assert(logLevelJson.contains(LogLevel::class.java.name))
        assert(themeJson.contains(Theme::class.java.name))

        // Deserialize and verify types are maintained
        val deserializedLogLevel = adapter.fromJson(logLevelJson) as FlagValue.EnumValue
        val deserializedTheme = adapter.fromJson(themeJson) as FlagValue.EnumValue

        assertEquals(LogLevel::class.java.name, deserializedLogLevel.enumClassName)
        assertEquals(Theme::class.java.name, deserializedTheme.enumClassName)
    }

    @Suppress("EnumNaming")
    enum class SpecialEnum {
        VALUE_WITH_UNDERSCORE,
        VALUE123,
        `VALUE-WITH-DASH`
    }

    @Test
    fun `EnumValue handles enum with special characters in name`() {

        val specialValue = FlagValue.from(SpecialEnum.VALUE_WITH_UNDERSCORE)
        assert(specialValue is FlagValue.EnumValue)

        val json = adapter.toJson(specialValue)
        val deserialized = adapter.fromJson(json)

        assert(deserialized is FlagValue.EnumValue)
        deserialized as FlagValue.EnumValue
        assertEquals("VALUE_WITH_UNDERSCORE", deserialized.value)
    }

    @Test
    fun `FlagValue from throws for unsupported types after adding enum support`() {
        // Enums should now be supported, but other complex types should still fail
        data class UnsupportedType(val value: String)

        assertThrows(IllegalArgumentException::class.java) {
            FlagValue.from(UnsupportedType("test"))
        }
    }

    @Test
    fun `EnumValue JSON format is consistent with other FlagValue types`() {
        val boolValue = FlagValue.BooleanValue(true)
        val enumValue = FlagValue.EnumValue("INFO", LogLevel::class.java.name)

        val boolJson = adapter.toJson(boolValue)
        val enumJson = adapter.toJson(enumValue)

        // Both should have "type" and "value" fields
        assert(boolJson.contains("\"type\""))
        assert(boolJson.contains("\"value\""))
        assert(enumJson.contains("\"type\""))
        assert(enumJson.contains("\"value\""))

        // Enum has additional enumClassName field
        assert(enumJson.contains("\"enumClassName\""))
        assert(!boolJson.contains("\"enumClassName\""))
    }
}
