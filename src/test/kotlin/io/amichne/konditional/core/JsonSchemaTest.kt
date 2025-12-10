package io.amichne.konditional.core

import io.amichne.konditional.core.dsl.json.jsonObject
import io.amichne.konditional.core.types.json.JsonSchema
import io.amichne.konditional.core.types.json.JsonValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test suite for JSON schema types.
 */
class JsonSchemaTest {

    enum class Theme {
        LIGHT, DARK, AUTO
    }

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    @Test
    fun `can create simple JSON object schema`() {
        val schema = jsonObject {
            fieldRaw("name", required = true) { string() }
            fieldRaw("enabled") { boolean() }
        }

        assertEquals(2, schema.fields.size)
        assertEquals(setOf("name"), schema.required)
        assertTrue(schema.fields["name"]?.required == true)
        assertFalse(schema.fields["enabled"]?.required == true)
    }

    @Test
    fun `can create nested JSON object schema`() {
        val schema = jsonObject {
            fieldRaw("user") {
                jsonObject {
                    fieldRaw("id") { int() }
                    fieldRaw("email") { string() }
                }
            }
            fieldRaw("settings") {
                jsonObject {
                    fieldRaw("theme") { enum<Theme>() }
                    fieldRaw("notifications") { boolean() }
                }
            }
        }

        assertEquals(2, schema.fields.size)
        val userSchema = schema.fields["user"]?.schema as? JsonSchema.ObjectSchema
        assertTrue(userSchema != null)
        assertEquals(2, userSchema?.fields?.size)
    }

    @Test
    fun `can create array schema`() {
        val schema = jsonObject {
            fieldRaw("tags") { array { string() } }
            fieldRaw("scores") { array { int() } }
        }

        assertEquals(2, schema.fields.size)
        val tagsSchema = schema.fields["tags"]?.schema as? JsonSchema.ArraySchema<*>
        assertTrue(tagsSchema != null)
        assertTrue(tagsSchema?.elementSchema is JsonSchema.StringSchema)
    }

    @Test
    fun `can create complex user config schema`() {
        val userConfigSchema = jsonObject {
            fieldRaw("userId") { string() }
            fieldRaw("theme") { enum<Theme>() }
            fieldRaw("logLevel") { enum<LogLevel>() }
            fieldRaw("notifications") {
                jsonObject {
                    fieldRaw("enabled") { boolean() }
                    fieldRaw("frequency") { string() }
                }
            }
            fieldRaw("favoriteCategories") { array { string() } }
        }

        // Verify schema structure
        assertEquals(5, userConfigSchema.fields.size)
        assertTrue(userConfigSchema.fields.containsKey("userId"))
        assertTrue(userConfigSchema.fields.containsKey("notifications"))

        val notificationsSchema = userConfigSchema.fields["notifications"]?.schema as? JsonSchema.ObjectSchema
        assertTrue(notificationsSchema != null)
        assertEquals(2, notificationsSchema?.fields?.size)
    }

    @Test
    fun `JsonObject with schema validates on construction`() {
        val schema = jsonObject {
            fieldRaw("id", required = true) { int() }
        }

        // Valid object should construct successfully
        val validObj = JsonValue.JsonObject(
            mapOf("id" to JsonValue.JsonNumber(123.0)),
            schema
        )
        assertEquals(JsonValue.JsonNumber(123.0), validObj["id"])

        // Invalid object should throw on construction
        assertThrows(IllegalArgumentException::class.java) {
            JsonValue.JsonObject(
                mapOf("name" to JsonValue.JsonString("test")),  // Missing required "id"
                schema
            )
        }
    }

    @Test
    fun `JsonArray with schema validates on construction`() {
        val elementSchema = JsonSchema.StringSchema

        // Valid array should construct successfully
        val validArray = JsonValue.JsonArray(
            listOf(
                JsonValue.JsonString("one"),
                JsonValue.JsonString("two")
            ),
            elementSchema
        )
        assertEquals(2, validArray.size)

        // Invalid array should throw on construction
        assertThrows(IllegalArgumentException::class.java) {
            JsonValue.JsonArray(
                listOf(
                    JsonValue.JsonString("one"),
                    JsonValue.JsonNumber(2.0)  // Wrong type
                ),
                elementSchema
            )
        }
    }

    @Test
    fun `enum schema validates string representation`() {
        val schema = JsonSchema.enum<Theme>()

        // Valid enum value
        val validResult = JsonValue.JsonString("DARK").validate(schema)
        assertTrue(validResult.isValid)

        // Invalid enum value
        val invalidResult = JsonValue.JsonString("INVALID_THEME").validate(schema)
        assertTrue(invalidResult.isInvalid)
    }

    @Test
    fun `deeply nested objects schema can be created`() {
        val schema = jsonObject {
            fieldRaw("level1") {
                jsonObject {
                    fieldRaw("level2") {
                        jsonObject {
                            fieldRaw("level3") {
                                jsonObject {
                                    fieldRaw("value") { string() }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verify deeply nested schema structure
        assertEquals(1, schema.fields.size)
        val level1Schema = schema.fields["level1"]?.schema as? JsonSchema.ObjectSchema
        assertTrue(level1Schema != null)

        val level2Schema = level1Schema?.fields?.get("level2")?.schema as? JsonSchema.ObjectSchema
        assertTrue(level2Schema != null)

        val level3Schema = level2Schema?.fields?.get("level3")?.schema as? JsonSchema.ObjectSchema
        assertTrue(level3Schema != null)
        assertTrue(level3Schema?.fields?.containsKey("value") == true)
    }
}
