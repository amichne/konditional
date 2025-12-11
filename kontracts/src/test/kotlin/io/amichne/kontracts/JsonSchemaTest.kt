package io.amichne.kontracts

import io.amichne.kontracts.dsl.buildJsonArray
import io.amichne.kontracts.dsl.buildJsonObject
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * Test suite for JSON schema and value types.
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
            field("id", required = true) { int() }
            field("name", required = true) { string() }
            field("enabled") { boolean() }
        }

        assertEquals(3, schema.fields.size)
        assertTrue(schema.fields["id"]?.required == true)
        assertTrue(schema.fields["name"]?.required == true)
        assertFalse(schema.fields["enabled"]?.required == true)
    }

    @Test
    fun `can create nested JSON object schema`() {
        val schema = jsonObject {
            field("user", required = true) {
                jsonObject {
                    field("id") { int() }
                    field("email") { string() }
                }
            }
            field("settings") {
                jsonObject {
                    field("theme") { enum<Theme>() }
                    field("notifications") { boolean() }
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
            field("tags") { array { string() } }
            field("scores") { array { int() } }
        }

        assertEquals(2, schema.fields.size)
        val tagsSchema = schema.fields["tags"]?.schema as? JsonSchema.ArraySchema
        assertTrue(tagsSchema != null)
        assertTrue(tagsSchema?.elementSchema is JsonSchema.StringSchema)
    }

    @Test
    fun `can build simple JSON object value`() {
        val obj = buildJsonObject {
            "id" to 123
            "name" to "John Doe"
            "enabled" to true
        }

        assertEquals(3, obj.fields.size)
        assertEquals(JsonValue.JsonNumber(123.0), obj["id"])
        assertEquals(JsonValue.JsonString("John Doe"), obj["name"])
        assertEquals(JsonValue.JsonBoolean(true), obj["enabled"])
    }

    @Test
    fun `can build nested JSON object value`() {
        val obj = buildJsonObject {
            "user" to buildJsonObject {
                "id" to 456
                "email" to "user@example.com"
            }
            "settings" to buildJsonObject {
                "theme" to Theme.DARK
                "notifications" to false
            }
        }

        val user = obj["user"] as? JsonValue.JsonObject
        assertTrue(user != null)
        assertEquals(JsonValue.JsonNumber(456.0), user?.fields["id"])

        val settings = obj["settings"] as? JsonValue.JsonObject
        assertTrue(settings != null)
        assertEquals(JsonValue.JsonString("DARK"), settings?.fields["theme"])
    }

    @Test
    fun `can build JSON arrays`() {
        val stringArray = buildJsonArray("one", "two", "three")
        assertEquals(3, stringArray.size)
        assertEquals(JsonValue.JsonString("one"), stringArray[0])

        val intArray = buildJsonArray(1, 2, 3, 4, 5)
        assertEquals(5, intArray.size)
        assertEquals(JsonValue.JsonNumber(1.0), intArray[0])

        val boolArray = buildJsonArray(true, false, true)
        assertEquals(3, boolArray.size)
        assertEquals(JsonValue.JsonBoolean(true), boolArray[0])
    }

    @Test
    fun `JSON object validates against schema`() {
        val schema = jsonObject {
            field("id", required = true) { int() }
            field("name", required = true) { string() }
        }

        val validObj = buildJsonObject {
            "id" to 123
            "name" to "Test"
        }

        val result = validObj.validate(schema)
        assertTrue(result.isValid)
    }

    @Test
    fun `JSON object validation fails with missing required field`() {
        val schema = jsonObject {
            field("id", required = true) { int() }
            field("name", required = true) { string() }
        }

        val invalidObj = buildJsonObject {
            "id" to 123
            // Missing required "name" field
        }

        val result = invalidObj.validate(schema)
        assertTrue(result.isInvalid)
        assertTrue(result.getErrorMessage()?.contains("required") == true)
    }

    @Test
    fun `JSON object validation fails with wrong type`() {
        val schema = jsonObject {
            field("id") { int() }
            field("name") { string() }
        }

        val invalidObj = buildJsonObject {
            "id" to "not a number"  // Wrong type
            "name" to "Test"
        }

        val result = invalidObj.validate(schema)
        assertTrue(result.isInvalid)
    }

    @Test
    fun `JSON array validates against schema`() {
        val array = buildJsonArray("one", "two", "three")
        val result = array.validate(JsonSchema.ArraySchema(JsonSchema.StringSchema()))
        assertTrue(result.isValid)
    }

    @Test
    fun `JSON array validation fails with wrong element type`() {
        val array = buildJsonArray(
            JsonValue.JsonString("one"),
            JsonValue.JsonNumber(2.0),  // Wrong type
            JsonValue.JsonString("three")
        )

        val result = array.validate(JsonSchema.ArraySchema(JsonSchema.StringSchema()))
        assertTrue(result.isInvalid)
    }

    @Test
    fun `can access typed values from JSON object`() {
        val obj = buildJsonObject {
            "id" to 123
            "name" to "Test"
            "enabled" to true
        }

        assertEquals(123, obj.getTyped<Int>("id"))
        assertEquals("Test", obj.getTyped<String>("name"))
        assertEquals(true, obj.getTyped<Boolean>("enabled"))
    }

    @Test
    fun `typed access returns null for wrong type`() {
        val obj = buildJsonObject {
            "id" to 123
        }

        assertEquals(null, obj.getTyped<String>("id"))  // Wrong type
        assertEquals(null, obj.getTyped<Int>("nonexistent"))  // Missing field
    }

    @Test
    fun `can create complex user config schema`() {
        val userConfigSchema = jsonObject {
            field("userId", required = true) { string() }
            field("theme") { enum<Theme>() }
            field("logLevel") { enum<LogLevel>() }
            field("notifications") {
                jsonObject {
                    field("enabled") { boolean() }
                    field("frequency") { string() }
                }
            }
            field("favoriteCategories") { array { string() } }
        }

        val userConfig = buildJsonObject {
            "userId" to "user123"
            "theme" to Theme.DARK
            "logLevel" to LogLevel.INFO
            "notifications" to buildJsonObject {
                "enabled" to true
                "frequency" to "daily"
            }
            "favoriteCategories" to buildJsonArray("tech", "news", "sports")
        }

        val result = userConfig.validate(userConfigSchema)
        assertTrue(result.isValid)
    }

    @Test
    fun `can create array of objects`() {
        val userSchema = jsonObject {
            field("id") { int() }
            field("name") { string() }
        }

        val users = buildJsonArray(
            elements = listOf(
                buildJsonObject {
                    "id" to 1
                    "name" to "Alice"
                },
                buildJsonObject {
                    "id" to 2
                    "name" to "Bob"
                },
                buildJsonObject {
                    "id" to 3
                    "name" to "Charlie"
                }
            ),
            elementSchema = userSchema
        )

        assertEquals(3, users.size)
        val firstUser = users[0] as? JsonValue.JsonObject
        assertEquals(JsonValue.JsonNumber(1.0), firstUser?.fields["id"])
    }

    @Test
    fun `JsonObject with schema validates on construction`() {
        val schema = jsonObject {
            field("id", required = true) { int() }
        }

        // Valid object should construct successfully
        val validObj = JsonValue.JsonObject(
            mapOf("id" to JsonValue.JsonNumber(123.0)),
            schema
        )
        assertEquals(JsonValue.JsonNumber(123.0), validObj["id"])

        // Invalid object should throw on construction
        assertThrows<IllegalArgumentException> {
            JsonValue.JsonObject(
                mapOf("name" to JsonValue.JsonString("test")),  // Missing required "id"
                schema
            )
        }
    }

    @Test
    fun `JsonArray with schema validates on construction`() {
        val elementSchema = JsonSchema.StringSchema()

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
        assertThrows<IllegalArgumentException> {
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
        val schema = JsonSchema.EnumSchema(enumClass = Theme::class, values = Theme.values().toList())

        // Valid enum value
        val validResult = JsonValue.JsonString("DARK").validate(schema)
        assertTrue(validResult.isValid)

        // Invalid enum value
        val invalidResult = JsonValue.JsonString("INVALID_THEME").validate(schema)
        assertTrue(invalidResult.isInvalid)
    }

    @Test
    fun `deeply nested objects validate correctly`() {
        val schema = jsonObject {
            field("level1") {
                jsonObject {
                    field("level2") {
                        jsonObject {
                            field("level3") {
                                jsonObject {
                                    field("value") { string() }
                                }
                            }
                        }
                    }
                }
            }
        }

        val obj = buildJsonObject {
            "level1" to buildJsonObject {
                "level2" to buildJsonObject {
                    "level3" to buildJsonObject {
                        "value" to "deeply nested"
                    }
                }
            }
        }

        val result = obj.validate(schema)
        assertTrue(result.isValid)
    }
}
