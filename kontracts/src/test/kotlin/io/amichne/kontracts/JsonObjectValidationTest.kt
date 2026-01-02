package io.amichne.kontracts

import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for JsonObject validation against schemas.
 * Tests required fields, nested objects, unknown fields, and complex object structures.
 */
class JsonObjectValidationTest {

    // ========== Basic Object Validation ==========

    @Test
    fun `JsonObject validates successfully with all required fields`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "age" to FieldSchema(JsonSchema.int(), required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "age" to JsonNumber(30.0)
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject fails validation with missing required field`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "age" to FieldSchema(JsonSchema.int(), required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice")
                // Missing "age"
            )
        )

        val result = obj.validate(schema)

        assertFalse(result.isValid)
        assertEquals("Missing required fields: [age]", result.getErrorMessage())
    }

    @Test
    fun `JsonObject fails validation with unknown field`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "unknown" to JsonString("value")
            )
        )

        val result = obj.validate(schema)

        assertFalse(result.isValid)
        assertEquals("Unknown field 'unknown' in object", result.getErrorMessage())
    }

    @Test
    fun `JsonObject validates with optional fields present`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "nickname" to FieldSchema(JsonSchema.string(), required = false)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "nickname" to JsonString("Ali")
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject validates with optional fields absent`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "nickname" to FieldSchema(JsonSchema.string(), required = false)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice")
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject fails validation when field has wrong type`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "age" to FieldSchema(JsonSchema.int(), required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "age" to JsonString("not a number")
            )
        )

        val result = obj.validate(schema)

        assertFalse(result.isValid)
        assertTrue(result.getErrorMessage()?.contains("Field 'age':") == true)
    }

    @Test
    fun `JsonObject validation throws on construction with schema mismatch`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            JsonObject(
                fields = mapOf("age" to JsonNumber(30.0)),
                schema = schema
            )
        }

        assertTrue(exception.message?.contains("JsonObject does not match schema") == true)
    }

    // ========== Nested Object Validation ==========

    @Test
    fun `JsonObject validates nested objects successfully`() {
        val addressSchema = JsonSchema.obj(
            fields = mapOf(
                "street" to FieldSchema(JsonSchema.string(), required = true),
                "city" to FieldSchema(JsonSchema.string(), required = true)
            )
        )
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "address" to FieldSchema(addressSchema, required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "address" to JsonObject(
                    fields = mapOf(
                        "street" to JsonString("123 Main St"),
                        "city" to JsonString("Springfield")
                    )
                )
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject fails validation with invalid nested object`() {
        val addressSchema = JsonSchema.obj(
            fields = mapOf(
                "street" to FieldSchema(JsonSchema.string(), required = true),
                "city" to FieldSchema(JsonSchema.string(), required = true)
            )
        )
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "address" to FieldSchema(addressSchema, required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "address" to JsonObject(
                    fields = mapOf(
                        "street" to JsonString("123 Main St")
                        // Missing "city"
                    )
                )
            )
        )

        val result = obj.validate(schema)

        assertFalse(result.isValid)
        assertTrue(result.getErrorMessage()?.contains("Field 'address':") == true)
        assertTrue(result.getErrorMessage()?.contains("Missing required fields: [city]") == true)
    }

    @Test
    fun `JsonObject validates deeply nested objects`() {
        val countrySchema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "code" to FieldSchema(JsonSchema.string(), required = true)
            )
        )
        val addressSchema = JsonSchema.obj(
            fields = mapOf(
                "street" to FieldSchema(JsonSchema.string(), required = true),
                "country" to FieldSchema(countrySchema, required = true)
            )
        )
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "address" to FieldSchema(addressSchema, required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "address" to JsonObject(
                    fields = mapOf(
                        "street" to JsonString("123 Main St"),
                        "country" to JsonObject(
                            fields = mapOf(
                                "name" to JsonString("USA"),
                                "code" to JsonString("US")
                            )
                        )
                    )
                )
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    // ========== Field Access ==========

    @Test
    fun `JsonObject get operator retrieves field by name`() {
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "age" to JsonNumber(30.0)
            )
        )

        assertEquals(JsonString("Alice"), obj["name"])
        assertEquals(JsonNumber(30.0), obj["age"])
        assertNull(obj["unknown"])
    }

    @Test
    fun `JsonObject getTyped returns correctly typed values`() {
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "age" to JsonNumber(30.0),
                "active" to JsonBoolean(true),
                "metadata" to JsonNull
            )
        )

        assertEquals("Alice", obj.getTyped<String>("name"))
        assertEquals(30, obj.getTyped<Int>("age"))
        assertEquals(30.0, obj.getTyped<Double>("age"))
        assertEquals(true, obj.getTyped<Boolean>("active"))
        assertNull(obj.getTyped<String>("metadata"))
        assertNull(obj.getTyped<String>("unknown"))
    }

    @Test
    fun `JsonObject getTyped returns null for type mismatch`() {
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice")
            )
        )

        assertNull(obj.getTyped<Int>("name"))
        assertNull(obj.getTyped<Boolean>("name"))
    }

    @Test
    fun `JsonObject getTyped handles nested objects`() {
        val obj = JsonObject(
            fields = mapOf(
                "address" to JsonObject(
                    fields = mapOf(
                        "city" to JsonString("Springfield")
                    )
                )
            )
        )

        val address = obj.getTyped<JsonObject>("address")
        assertEquals("Springfield", address?.getTyped<String>("city"))
    }

    // ========== Empty Objects ==========

    @Test
    fun `JsonObject validates empty object with no required fields`() {
        val schema = JsonSchema.obj(fields = emptyMap())
        val obj = JsonObject(fields = emptyMap())

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject fails validation for empty object with required fields`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true)
            )
        )
        val obj = JsonObject(fields = emptyMap())

        val result = obj.validate(schema)

        assertFalse(result.isValid)
        assertEquals("Missing required fields: [name]", result.getErrorMessage())
    }

    // ========== Type Mismatch ==========

    @Test
    fun `JsonObject fails validation against non-object schema`() {
        val stringSchema = JsonSchema.string()
        val obj = JsonObject(
            fields = mapOf("name" to JsonString("Alice"))
        )

        val result = obj.validate(stringSchema)

        assertFalse(result.isValid)
        assertEquals("Expected ${stringSchema}, but got JsonObject", result.getErrorMessage())
    }

    // ========== Complex Scenarios ==========

    @Test
    fun `JsonObject validates with mixed required and optional fields`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "id" to FieldSchema(JsonSchema.int(), required = true),
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "email" to FieldSchema(JsonSchema.string(), required = false),
                "phone" to FieldSchema(JsonSchema.string(), required = false),
                "active" to FieldSchema(JsonSchema.boolean(), required = true)
            )
        )
        val obj = JsonObject(
            fields = mapOf(
                "id" to JsonNumber(1.0),
                "name" to JsonString("Alice"),
                "email" to JsonString("alice@example.com"),
                "active" to JsonBoolean(true)
                // "phone" is optional and omitted
            )
        )

        val result = obj.validate(schema)

        assertTrue(result.isValid)
    }

    @Test
    fun `JsonObject toString formats correctly`() {
        val obj = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "age" to JsonNumber(30.0)
            )
        )

        val str = obj.toString()

        assertTrue(str.contains("\"name\": \"Alice\""))
        assertTrue(str.contains("\"age\": 30.0"))
    }
}
