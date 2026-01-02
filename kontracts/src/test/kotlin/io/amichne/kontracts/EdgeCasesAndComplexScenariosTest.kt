package io.amichne.kontracts

import io.amichne.kontracts.dsl.asInt
import io.amichne.kontracts.dsl.asString
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.FieldSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.SchemaProvider
import io.amichne.kontracts.value.JsonArray
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNull
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import io.amichne.kontracts.value.JsonValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Edge cases and complex scenario tests for the Kontracts library.
 * Tests boundary conditions, deeply nested structures, and real-world usage patterns.
 */
class EdgeCasesAndComplexScenariosTest {

    // ========== Deeply Nested Structures ==========

    @Test
    fun `validates deeply nested object hierarchy`() {
        // Level 3: Country
        val countrySchema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "code" to FieldSchema(JsonSchema.string(minLength = 2, maxLength = 2), required = true)
            )
        )

        // Level 2: Address
        val addressSchema = JsonSchema.obj(
            fields = mapOf(
                "street" to FieldSchema(JsonSchema.string(), required = true),
                "city" to FieldSchema(JsonSchema.string(), required = true),
                "country" to FieldSchema(countrySchema, required = true)
            )
        )

        // Level 1: Person
        val personSchema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "address" to FieldSchema(addressSchema, required = true)
            )
        )

        val person = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "address" to JsonObject(
                    fields = mapOf(
                        "street" to JsonString("123 Main St"),
                        "city" to JsonString("Springfield"),
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

        val result = person.validate(personSchema)
        assertTrue(result.isValid)
    }

    @Test
    fun `fails validation in deeply nested structure`() {
        val countrySchema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "code" to FieldSchema(JsonSchema.string(minLength = 2, maxLength = 2), required = true)
            )
        )

        val addressSchema = JsonSchema.obj(
            fields = mapOf(
                "street" to FieldSchema(JsonSchema.string(), required = true),
                "country" to FieldSchema(countrySchema, required = true)
            )
        )

        val personSchema = JsonSchema.obj(
            fields = mapOf(
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "address" to FieldSchema(addressSchema, required = true)
            )
        )

        val person = JsonObject(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "address" to JsonObject(
                    fields = mapOf(
                        "street" to JsonString("123 Main St"),
                        "country" to JsonObject(
                            fields = mapOf(
                                "name" to JsonString("USA"),
                                "code" to JsonString("USA")  // Too long!
                            )
                        )
                    )
                )
            )
        )

        val result = person.validate(personSchema)
        assertFalse(result.isValid)
        assertTrue(result.getErrorMessage()?.contains("address") == true)
        assertTrue(result.getErrorMessage()?.contains("country") == true)
    }

    // ========== Arrays of Complex Objects ==========

    @Test
    fun `validates array of objects with nested arrays`() {
        val tagSchema = JsonSchema.array(JsonSchema.string())
        val itemSchema = JsonSchema.obj(
            fields = mapOf(
                "id" to FieldSchema(JsonSchema.int(), required = true),
                "name" to FieldSchema(JsonSchema.string(), required = true),
                "tags" to FieldSchema(tagSchema, required = true)
            )
        )
        val schema = JsonSchema.array(itemSchema)

        val items = JsonArray(
            elements = listOf(
                JsonObject(
                    fields = mapOf(
                        "id" to JsonNumber(1.0),
                        "name" to JsonString("Item 1"),
                        "tags" to JsonArray(elements = listOf(JsonString("tag1"), JsonString("tag2")))
                    )
                ),
                JsonObject(
                    fields = mapOf(
                        "id" to JsonNumber(2.0),
                        "name" to JsonString("Item 2"),
                        "tags" to JsonArray(elements = listOf(JsonString("tag3")))
                    )
                )
            )
        )

        val result = items.validate(schema)
        assertTrue(result.isValid)
    }

    @Test
    fun `fails validation in nested array element`() {
        val tagSchema = JsonSchema.array(JsonSchema.string(minLength = 3))
        val itemSchema = JsonSchema.obj(
            fields = mapOf(
                "id" to FieldSchema(JsonSchema.int(), required = true),
                "tags" to FieldSchema(tagSchema, required = true)
            )
        )
        val schema = JsonSchema.array(itemSchema)

        val items = JsonArray(
            elements = listOf(
                JsonObject(
                    fields = mapOf(
                        "id" to JsonNumber(1.0),
                        "tags" to JsonArray(elements = listOf(JsonString("tag")))
                    )
                ),
                JsonObject(
                    fields = mapOf(
                        "id" to JsonNumber(2.0),
                        "tags" to JsonArray(elements = listOf(JsonString("ab")))  // Too short
                    )
                )
            )
        )

        val result = items.validate(schema)
        assertFalse(result.isValid)
        assertTrue(result.getErrorMessage()?.contains("Element at index 1:") == true)
    }

    // ========== Boundary Value Testing ==========

    @Test
    fun `validates boundary values for numeric constraints`() {
        val schema = JsonSchema.int(minimum = 0, maximum = 100)

        assertTrue(JsonNumber(0.0).validate(schema).isValid)
        assertTrue(JsonNumber(100.0).validate(schema).isValid)
        assertFalse(JsonNumber(-1.0).validate(schema).isValid)
        assertFalse(JsonNumber(101.0).validate(schema).isValid)
    }

    @Test
    fun `validates boundary values for string length`() {
        val schema = JsonSchema.string(minLength = 5, maxLength = 10)

        assertTrue(JsonString("12345").validate(schema).isValid)
        assertTrue(JsonString("1234567890").validate(schema).isValid)
        assertFalse(JsonString("1234").validate(schema).isValid)
        assertFalse(JsonString("12345678901").validate(schema).isValid)
    }

    @Test
    fun `handles Double precision edge cases`() {
        val schema = JsonSchema.double(minimum = 0.0, maximum = 1.0)

        assertTrue(JsonNumber(0.0).validate(schema).isValid)
        assertTrue(JsonNumber(0.5).validate(schema).isValid)
        assertTrue(JsonNumber(1.0).validate(schema).isValid)
        assertTrue(JsonNumber(0.999999999).validate(schema).isValid)
        assertFalse(JsonNumber(1.000001).validate(schema).isValid)
    }

    // ========== Unicode and Special Characters ==========

    @Test
    fun `validates strings with unicode characters`() {
        val schema = JsonSchema.string()

        assertTrue(JsonString("Hello 世界").validate(schema).isValid)
        assertTrue(JsonString("Привет мир").validate(schema).isValid)
        assertTrue(JsonString("مرحبا").validate(schema).isValid)
        assertTrue(JsonString("🚀🌟").validate(schema).isValid)
    }

    @Test
    fun `validates patterns with special regex characters`() {
        val schema = JsonSchema.string(pattern = "^\\d{3}-\\d{2}-\\d{4}$")

        assertTrue(JsonString("123-45-6789").validate(schema).isValid)
        assertFalse(JsonString("12-345-6789").validate(schema).isValid)
        assertFalse(JsonString("abc-de-fghi").validate(schema).isValid)
    }

    @Test
    fun `validates email pattern with complex addresses`() {
        val schema = JsonSchema.string(
            pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )

        assertTrue(JsonString("user@example.com").validate(schema).isValid)
        assertTrue(JsonString("first.last@subdomain.example.com").validate(schema).isValid)
        assertTrue(JsonString("user+tag@example.co.uk").validate(schema).isValid)
        assertFalse(JsonString("invalid@").validate(schema).isValid)
        assertFalse(JsonString("@example.com").validate(schema).isValid)
        assertFalse(JsonString("user@.com").validate(schema).isValid)
    }

    // ========== Large Data Structures ==========

    @Test
    fun `validates large array of primitives`() {
        val schema = JsonSchema.array(JsonSchema.int())
        val largeArray = JsonArray(
            elements = (1..1000).map { JsonNumber(it.toDouble()) }
        )

        val result = largeArray.validate(schema)
        assertTrue(result.isValid)
    }

    @Test
    fun `validates object with many fields`() {
        val fields = (1..50).associate { index ->
            "field$index" to FieldSchema(JsonSchema.string(), required = true)
        }
        val schema = JsonSchema.obj(fields = fields)

        val obj = JsonObject(
            fields = (1..50).associate { index ->
                "field$index" to JsonString("value$index")
            }
        )

        val result = obj.validate(schema)
        assertTrue(result.isValid)
    }

    // ========== Real-World Configuration Examples ==========

    @Test
    fun `validates REST API endpoint configuration`() {
        data class Endpoint(
            val path: String,
            val method: String,
            val timeout: Int,
            val retries: Int,
            val rateLimit: Double?,
            val authenticated: Boolean
        ) : SchemaProvider<ObjectSchema> {
            override val schema = schemaRoot {
                ::path of {
                    pattern = "^/[a-zA-Z0-9/_-]*$"
                    description = "API endpoint path"
                }
                ::method of {
                    enum = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
                    description = "HTTP method"
                }
                ::timeout of {
                    minimum = 100
                    maximum = 60000
                    description = "Request timeout in milliseconds"
                }
                ::retries of {
                    minimum = 0
                    maximum = 5
                    description = "Number of retry attempts"
                }
                ::rateLimit of {
                    minimum = 0.0
                    maximum = 1000.0
                    description = "Requests per second limit"
                }
                ::authenticated of {
                    description = "Requires authentication"
                }
            }
        }

        val endpoint = Endpoint(
            path = "/api/users",
            method = "GET",
            timeout = 5000,
            retries = 3,
            rateLimit = 100.0,
            authenticated = true
        )

        assertEquals(6, endpoint.schema.fields.size)
        assertTrue(endpoint.schema.fields["path"]!!.required)
        assertFalse(endpoint.schema.fields["rateLimit"]!!.required)
    }

    @Test
    fun `validates database connection configuration`() {
        data class DatabaseConfig(
            val host: String,
            val port: Int,
            val database: String,
            val username: String,
            val maxConnections: Int,
            val connectionTimeout: Int,
            val ssl: Boolean
        ) : SchemaProvider<ObjectSchema> {
            override val schema = schemaRoot {
                ::host of {
                    minLength = 1
                    description = "Database host"
                }
                ::port of {
                    minimum = 1
                    maximum = 65535
                    description = "Database port"
                }
                ::database of {
                    pattern = "^[a-zA-Z0-9_-]+$"
                    description = "Database name"
                }
                ::username of {
                    minLength = 1
                    description = "Database username"
                }
                ::maxConnections of {
                    minimum = 1
                    maximum = 1000
                    default = 10
                    description = "Maximum connection pool size"
                }
                ::connectionTimeout of {
                    minimum = 1000
                    maximum = 300000
                    default = 30000
                    description = "Connection timeout in milliseconds"
                }
                ::ssl of {
                    default = true
                    description = "Enable SSL/TLS"
                }
            }
        }

        val config = DatabaseConfig(
            host = "localhost",
            port = 5432,
            database = "myapp",
            username = "admin",
            maxConnections = 20,
            connectionTimeout = 30000,
            ssl = true
        )

        assertEquals(7, config.schema.fields.size)
        assertEquals(10, config.schema.fields["maxConnections"]!!.schema.default)
    }

    // ========== Mixed Type Collections ==========

    @Test
    fun `validates object with heterogeneous field types`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "string" to FieldSchema(JsonSchema.string(), required = true),
                "number" to FieldSchema(JsonSchema.double(), required = true),
                "integer" to FieldSchema(JsonSchema.int(), required = true),
                "boolean" to FieldSchema(JsonSchema.boolean(), required = true),
                "null" to FieldSchema(JsonSchema.nullSchema(), required = true),
                "array" to FieldSchema(JsonSchema.array(JsonSchema.string()), required = true),
                "object" to FieldSchema(
                    JsonSchema.obj(
                        fields = mapOf(
                            "nested" to FieldSchema(JsonSchema.string(), required = true)
                        )
                    ),
                    required = true
                )
            )
        )

        val obj = JsonObject(
            fields = mapOf(
                "string" to JsonString("value"),
                "number" to JsonNumber(42.5),
                "integer" to JsonNumber(42.0),
                "boolean" to JsonBoolean(true),
                "null" to JsonNull,
                "array" to JsonArray(elements = listOf(JsonString("item"))),
                "object" to JsonObject(
                    fields = mapOf(
                        "nested" to JsonString("nested value")
                    )
                )
            )
        )

        val result = obj.validate(schema)
        assertTrue(result.isValid)
    }

    // ========== JsonValue Factory Methods ==========

    @Test
    fun `JsonValue factory methods create correct types`() {
        assertEquals(JsonBoolean(true), JsonValue.from(true))
        assertEquals(JsonString("test"), JsonValue.from("test"))
        assertEquals(JsonNumber(42.0), JsonValue.from(42))
        assertEquals(JsonNumber(42.5), JsonValue.from(42.5))
    }

    @Test
    fun `JsonValue obj factory creates JsonObject`() {
        val obj = JsonValue.obj(
            fields = mapOf(
                "name" to JsonString("Alice"),
                "age" to JsonNumber(30.0)
            )
        )

        assertEquals("Alice", obj.getTyped<String>("name"))
        assertEquals(30, obj.getTyped<Int>("age"))
    }

    @Test
    fun `JsonValue array factory creates JsonArray`() {
        val arr = JsonValue.array(
            elements = listOf(
                JsonString("a"),
                JsonString("b"),
                JsonString("c")
            )
        )

        assertEquals(3, arr.size)
        assertEquals(JsonString("a"), arr[0])
    }

    // ========== Error Message Quality ==========

    @Test
    fun `error messages are descriptive and actionable`() {
        val schema = JsonSchema.string(minLength = 5, maxLength = 10, pattern = "^[A-Z]+$")

        val tooShort = JsonString("AB")
        val result1 = tooShort.validate(schema)
        assertEquals("String length 2 is less than minimum length 5", result1.getErrorMessage())

        val tooLong = JsonString("ABCDEFGHIJK")
        val result2 = tooLong.validate(schema)
        assertEquals("String length 11 is greater than maximum length 10", result2.getErrorMessage())

        val wrongPattern = JsonString("abcde")
        val result3 = wrongPattern.validate(schema)
        assertEquals("String 'abcde' does not match pattern ^[A-Z]+$", result3.getErrorMessage())
    }

    @Test
    fun `nested error messages provide path context`() {
        val schema = JsonSchema.obj(
            fields = mapOf(
                "user" to FieldSchema(
                    JsonSchema.obj(
                        fields = mapOf(
                            "age" to FieldSchema(JsonSchema.int(minimum = 18), required = true)
                        )
                    ),
                    required = true
                )
            )
        )

        val obj = JsonObject(
            fields = mapOf(
                "user" to JsonObject(
                    fields = mapOf(
                        "age" to JsonNumber(16.0)
                    )
                )
            )
        )

        val result = obj.validate(schema)
        assertFalse(result.isValid)
        val errorMsg = result.getErrorMessage()!!
        assertTrue(errorMsg.contains("Field 'user':"))
        assertTrue(errorMsg.contains("Field 'age':"))
    }

    // ========== Custom Type Integration ==========

    data class CustomId(val value: String)
    data class CustomCount(val value: Int)

    @Test
    fun `custom types integrate seamlessly with validation`() {
        data class Config(
            val id: CustomId,
            val count: CustomCount,
            val name: String
        ) : SchemaProvider<ObjectSchema> {
            override val schema = schemaRoot {
                ::id asString {
                    represent = { this.value }
                    pattern = "^ID-[0-9]{6}$"
                }
                ::count asInt {
                    represent = { this.value }
                    minimum = 1
                    maximum = 1000
                }
                ::name of {
                    minLength = 1
                }
            }
        }

        val config = Config(
            id = CustomId("ID-123456"),
            count = CustomCount(100),
            name = "Test"
        )

        assertEquals(3, config.schema.fields.size)
    }
}
