package io.amichne.konditional.serialization

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for SerializerRegistry.
 *
 * Verifies:
 * - Registration and lookup of custom serializers
 * - Built-in primitive serializers
 * - Thread safety
 * - Error handling
 */
class SerializerRegistryTest {

    // Test custom types
    data class RetryPolicy(
        val maxAttempts: Int = 3,
        val backoffMs: Double = 1000.0
    ) : KotlinEncodeable<ObjectSchema> {
        override val schema = schemaRoot {
            ::maxAttempts of { minimum = 1 }
            ::backoffMs of { minimum = 0.0 }
        }

        companion object {
            val serializer = object : TypeSerializer<RetryPolicy> {
                override fun encode(value: RetryPolicy): JsonObject =
                    jsonObject {
                        "maxAttempts" to value.maxAttempts
                        "backoffMs" to value.backoffMs
                    }

                override fun decode(json: io.amichne.kontracts.value.JsonValue): ParseResult<RetryPolicy> =
                    when (json) {
                        is JsonObject -> {
                            val maxAttempts = json.fields["maxAttempts"]?.asInt()
                            val backoffMs = json.fields["backoffMs"]?.asDouble()

                            if (maxAttempts != null && backoffMs != null) {
                                ParseResult.Success(RetryPolicy(maxAttempts, backoffMs))
                            } else {
                                ParseResult.Failure(ParseError.InvalidSnapshot("Missing required fields"))
                            }
                        }
                        else -> ParseResult.Failure(ParseError.InvalidSnapshot("Expected JsonObject"))
                    }
            }
        }
    }

    enum class Theme {
        LIGHT, DARK
    }

    @AfterEach
    fun cleanup() {
        SerializerRegistry.clear()
    }

    // ========== Registration Tests ==========

    @Test
    fun `register custom serializer`() {
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        val retrieved = SerializerRegistry.get(RetryPolicy::class)
        assertNotNull(retrieved)
        assertEquals(1, SerializerRegistry.size())
    }

    @Test
    fun `cannot register built-in types`() {
        val dummySerializer = object : TypeSerializer<String> {
            override fun encode(value: String) = JsonString(value)
            override fun decode(json: io.amichne.kontracts.value.JsonValue) = ParseResult.Success("")
        }

        val exception = assertThrows<IllegalArgumentException> {
            SerializerRegistry.register(String::class, dummySerializer)
        }

        assertTrue(exception.message!!.contains("Cannot register serializer for built-in type"))
    }

    @Test
    fun `registration is idempotent - last wins`() {
        val serializer1 = object : TypeSerializer<RetryPolicy> {
            override fun encode(value: RetryPolicy) = jsonObject { "version" to 1 }
            override fun decode(json: io.amichne.kontracts.value.JsonValue) =
                ParseResult.Success(RetryPolicy())
        }

        val serializer2 = object : TypeSerializer<RetryPolicy> {
            override fun encode(value: RetryPolicy) = jsonObject { "version" to 2 }
            override fun decode(json: io.amichne.kontracts.value.JsonValue) =
                ParseResult.Success(RetryPolicy())
        }

        SerializerRegistry.register(RetryPolicy::class, serializer1)
        SerializerRegistry.register(RetryPolicy::class, serializer2)

        assertEquals(1, SerializerRegistry.size()) // Only one entry

        val retrieved = SerializerRegistry.get(RetryPolicy::class)!!
        val encoded = retrieved.encode(RetryPolicy())
        assertEquals(JsonNumber(2.0), (encoded as JsonObject).fields["version"])
    }

    @Test
    fun `get returns null for unregistered type`() {
        val retrieved = SerializerRegistry.get(RetryPolicy::class)
        assertNull(retrieved)
    }

    // ========== Encoding Tests ==========

    @Test
    fun `encode Boolean using built-in serializer`() {
        val result = SerializerRegistry.encode(true)
        assertEquals(JsonBoolean(true), result)
    }

    @Test
    fun `encode String using built-in serializer`() {
        val result = SerializerRegistry.encode("hello")
        assertEquals(JsonString("hello"), result)
    }

    @Test
    fun `encode Int using built-in serializer`() {
        val result = SerializerRegistry.encode(42)
        assertEquals(JsonNumber(42.0), result)
    }

    @Test
    fun `encode Double using built-in serializer`() {
        val result = SerializerRegistry.encode(3.14)
        assertEquals(JsonNumber(3.14), result)
    }

    @Test
    fun `encode Enum using built-in serializer`() {
        val result = SerializerRegistry.encode(Theme.DARK)
        assertEquals(JsonString("DARK"), result)
    }

    @Test
    fun `encode custom type with registered serializer`() {
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        val policy = RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)
        val result = SerializerRegistry.encode(policy)

        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        assertEquals(JsonNumber(5.0), obj.fields["maxAttempts"])
        assertEquals(JsonNumber(2000.0), obj.fields["backoffMs"])
    }

    @Test
    fun `encode custom type without registered serializer throws`() {
        val policy = RetryPolicy()
        val exception = assertThrows<IllegalStateException> {
            SerializerRegistry.encode(policy)
        }

        assertTrue(exception.message!!.contains("No serializer registered"))
    }

    // ========== Decoding Tests ==========

    @Test
    fun `decode Boolean using built-in serializer`() {
        val result = SerializerRegistry.decode(Boolean::class, JsonBoolean(true))

        assertTrue(result is ParseResult.Success)
        assertEquals(true, (result as ParseResult.Success).value)
    }

    @Test
    fun `decode String using built-in serializer`() {
        val result = SerializerRegistry.decode(String::class, JsonString("hello"))

        assertTrue(result is ParseResult.Success)
        assertEquals("hello", (result as ParseResult.Success).value)
    }

    @Test
    fun `decode Int using built-in serializer`() {
        val result = SerializerRegistry.decode(Int::class, JsonNumber(42.0))

        assertTrue(result is ParseResult.Success)
        assertEquals(42, (result as ParseResult.Success).value)
    }

    @Test
    fun `decode Double using built-in serializer`() {
        val result = SerializerRegistry.decode(Double::class, JsonNumber(3.14))

        assertTrue(result is ParseResult.Success)
        assertEquals(3.14, (result as ParseResult.Success).value)
    }

    @Test
    fun `decode Enum using built-in serializer`() {
        val result = SerializerRegistry.decode(Theme::class, JsonString("DARK"))

        assertTrue(result is ParseResult.Success)
        assertEquals(Theme.DARK, (result as ParseResult.Success).value)
    }

    @Test
    fun `decode Enum with invalid constant name fails`() {
        val result = SerializerRegistry.decode(Theme::class, JsonString("INVALID"))

        assertTrue(result is ParseResult.Failure)
        val error = (result as ParseResult.Failure).error
        assertTrue(error is ParseError.InvalidSnapshot)
        assertTrue(error.message.contains("Unknown enum constant"))
    }

    @Test
    fun `decode custom type with registered serializer`() {
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        val json = jsonObject {
            "maxAttempts" to 5
            "backoffMs" to 2000.0
        }

        val result = SerializerRegistry.decode(RetryPolicy::class, json)

        assertTrue(result is ParseResult.Success)
        val policy = (result as ParseResult.Success).value
        assertEquals(5, policy.maxAttempts)
        assertEquals(2000.0, policy.backoffMs)
    }

    @Test
    fun `decode custom type without registered serializer fails`() {
        val json = jsonObject {
            "maxAttempts" to 5
            "backoffMs" to 2000.0
        }

        val result = SerializerRegistry.decode(RetryPolicy::class, json)

        assertTrue(result is ParseResult.Failure)
        val error = (result as ParseResult.Failure).error
        assertTrue(error is ParseError.InvalidSnapshot)
        assertTrue(error.message.contains("No serializer registered"))
    }

    @Test
    fun `decode type with wrong JSON type fails`() {
        val result = SerializerRegistry.decode(Int::class, JsonString("not a number"))

        assertTrue(result is ParseResult.Failure)
        val error = (result as ParseResult.Failure).error
        assertTrue(error is ParseError.InvalidSnapshot)
        assertTrue(error.message.contains("Expected JsonNumber"))
    }

    // ========== Decode by Class Name Tests ==========

    @Test
    fun `decodeByClassName with registered serializer`() {
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        val json = jsonObject {
            "maxAttempts" to 5
            "backoffMs" to 2000.0
        }

        val className = RetryPolicy::class.java.name
        val result = SerializerRegistry.decodeByClassName(className, json)

        assertTrue(result is ParseResult.Success)
        val policy = (result as ParseResult.Success).value as RetryPolicy
        assertEquals(5, policy.maxAttempts)
        assertEquals(2000.0, policy.backoffMs)
    }

    @Test
    fun `decodeByClassName with enum`() {
        val className = Theme::class.java.name
        val result = SerializerRegistry.decodeByClassName(className, JsonString("DARK"))

        assertTrue(result is ParseResult.Success)
        assertEquals(Theme.DARK, (result as ParseResult.Success).value)
    }

    @Test
    fun `decodeByClassName with invalid class name fails`() {
        val result = SerializerRegistry.decodeByClassName("com.invalid.ClassName", JsonString("value"))

        assertTrue(result is ParseResult.Failure)
        val error = (result as ParseResult.Failure).error
        assertTrue(error is ParseError.InvalidSnapshot)
        assertTrue(error.message.contains("Failed to load class"))
    }

    @Test
    fun `decodeByClassName without registered serializer fails`() {
        val className = RetryPolicy::class.java.name
        val json = jsonObject {
            "maxAttempts" to 5
            "backoffMs" to 2000.0
        }

        val result = SerializerRegistry.decodeByClassName(className, json)

        assertTrue(result is ParseResult.Failure)
        val error = (result as ParseResult.Failure).error
        assertTrue(error is ParseError.InvalidSnapshot)
        assertTrue(error.message.contains("No serializer registered"))
    }

    // ========== Round-trip Tests ==========

    @Test
    fun `round-trip encode and decode preserves value`() {
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        val original = RetryPolicy(maxAttempts = 7, backoffMs = 3000.0)

        val encoded = SerializerRegistry.encode(original)
        val decoded = SerializerRegistry.decode(RetryPolicy::class, encoded)

        assertTrue(decoded is ParseResult.Success)
        val result = (decoded as ParseResult.Success).value
        assertEquals(original.maxAttempts, result.maxAttempts)
        assertEquals(original.backoffMs, result.backoffMs)
    }

    @Test
    fun `round-trip with all primitive types`() {
        assertEquals(
            true,
            (SerializerRegistry.decode(
                Boolean::class,
                SerializerRegistry.encode(true)
            ) as ParseResult.Success).value
        )
        assertEquals(
            "test",
            (SerializerRegistry.decode(
                String::class,
                SerializerRegistry.encode("test")
            ) as ParseResult.Success).value
        )
        assertEquals(
            42,
            (SerializerRegistry.decode(Int::class, SerializerRegistry.encode(42)) as ParseResult.Success).value
        )
        assertEquals(
            3.14,
            (SerializerRegistry.decode(
                Double::class,
                SerializerRegistry.encode(3.14)
            ) as ParseResult.Success).value
        )
        assertEquals(
            Theme.LIGHT,
            (SerializerRegistry.decode(
                Theme::class,
                SerializerRegistry.encode(Theme.LIGHT)
            ) as ParseResult.Success).value
        )
    }

    // ========== JsonObject Builder Tests ==========

    @Test
    fun `jsonObject DSL builds correct structure`() {
        val obj = jsonObject {
            "name" to "Alice"
            "age" to 30
            "active" to true
            "score" to 95.5
        }

        assertEquals(JsonString("Alice"), obj.fields["name"])
        assertEquals(JsonNumber(30.0), obj.fields["age"])
        assertEquals(JsonBoolean(true), obj.fields["active"])
        assertEquals(JsonNumber(95.5), obj.fields["score"])
    }

    @Test
    fun `jsonObject with nested objects`() {
        val obj = jsonObject {
            "settings" to jsonObject {
                "theme" to "dark"
                "notifications" to false
            }
        }

        val nested = obj.fields["settings"] as JsonObject
        assertEquals(JsonString("dark"), nested.fields["theme"])
        assertEquals(JsonBoolean(false), nested.fields["notifications"])
    }

    @Test
    fun `jsonArray constructs array correctly`() {
        val arr = jsonArray("a", "b", "c")

        assertEquals(3, arr.elements.size)
        assertEquals(JsonString("a"), arr.elements[0])
        assertEquals(JsonString("b"), arr.elements[1])
        assertEquals(JsonString("c"), arr.elements[2])
    }

    @Test
    fun `JsonValue extension functions extract correct types`() {
        assertEquals(42, JsonNumber(42.5).asInt())
        assertEquals(3.14, JsonNumber(3.14).asDouble())
        assertEquals("hello", JsonString("hello").asString())
        assertEquals(true, JsonBoolean(true).asBoolean())

        assertNull(JsonString("not a number").asInt())
        assertNull(JsonBoolean(true).asDouble())
    }
}
