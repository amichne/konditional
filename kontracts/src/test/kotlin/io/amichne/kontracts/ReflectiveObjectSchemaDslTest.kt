package io.amichne.kontracts

import io.amichne.kontracts.dsl.reflectiveSchema
import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReflectiveObjectSchemaDslTest {
    data class RetryConfig(
        val enabled: Boolean = true,
        val maxRetries: Int? = 3,
    )

    @Test
    fun `reflectiveSchema stores property references with compile-time ownership`() {
        val schema =
            reflectiveSchema<RetryConfig> {
                required(
                    property = RetryConfig::enabled,
                    schema = JsonSchema.boolean(description = "Enable retries"),
                )
                optional(
                    property = RetryConfig::maxRetries,
                    schema = JsonSchema.int(minimum = 0, nullable = true),
                )
            }

        val enabledField = assertNotNull(schema.field(RetryConfig::enabled))
        assertEquals("enabled", enabledField.name)
        assertTrue(enabledField.required)
        assertIs<BooleanSchema>(enabledField.schema)

        val retriesField = assertNotNull(schema.field(RetryConfig::maxRetries))
        assertEquals("maxRetries", retriesField.name)
        assertFalse(retriesField.required)
        val retriesSchema = assertIs<IntSchema>(retriesField.schema)
        assertTrue(retriesSchema.nullable)
    }

    @Test
    fun `reflectiveSchema remains ObjectTraits compatible for map-based consumers`() {
        val schema =
            reflectiveSchema<RetryConfig> {
                required(RetryConfig::enabled, JsonSchema.boolean())
                optional(RetryConfig::maxRetries, JsonSchema.int(nullable = true))
            }

        assertEquals(setOf("enabled", "maxRetries"), schema.fields.keys)
        assertTrue(schema.fields.getValue("enabled").required)
        assertFalse(schema.fields.getValue("maxRetries").required)
    }

    @Test
    fun `reflectiveSchema rejects duplicate property registrations`() {
        val error =
            assertFailsWith<IllegalStateException> {
                reflectiveSchema<RetryConfig> {
                    required(RetryConfig::enabled, JsonSchema.boolean())
                    optional(RetryConfig::enabled, JsonSchema.boolean(nullable = true))
                }
            }

        val message = error.message ?: ""
        assertTrue(message.contains("already defines property 'enabled'"))
    }
}
