package io.amichne.konditional.openapi

import io.amichne.kontracts.dsl.reflectiveSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.OneOfSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenApiSchemaConverterTest {
    private data class SampleSchemaPayload(
        val name: String,
        val enabled: Boolean?,
    )

    @Test
    fun `toSchema converts reflective object schemas into OpenAPI object definitions`() {
        val schema =
            reflectiveSchema<SampleSchemaPayload> {
                required(SampleSchemaPayload::name, JsonSchema.string(minLength = 1))
                optional(SampleSchemaPayload::enabled, JsonSchema.boolean(nullable = true))
            }

        val converted = OpenApiSchemaConverter.toSchema(schema)
        assertEquals("object", converted["type"])
        assertEquals(false, converted["additionalProperties"])

        val properties = assertIs<Map<*, *>>(converted["properties"])
        val name = assertIs<Map<*, *>>(properties["name"])
        val enabled = assertIs<Map<*, *>>(properties["enabled"])
        val required = assertIs<List<*>>(converted["required"])

        assertEquals("string", name["type"])
        assertEquals(1, name["minLength"])
        assertEquals("boolean", enabled["type"])
        assertEquals(true, enabled["nullable"])
        assertEquals(listOf("name"), required)
    }

    @Test
    fun `toSchema includes oneOf discriminator mappings`() {
        val schema =
            JsonSchema.oneOf(
                options = listOf(JsonSchema.string(), JsonSchema.int()),
                discriminator =
                    OneOfSchema.Discriminator(
                        propertyName = "type",
                        mapping = mapOf("STRING" to "#/components/schemas/StringValue"),
                    ),
            )

        val converted = OpenApiSchemaConverter.toSchema(schema)
        val discriminator = assertIs<Map<*, *>>(converted["discriminator"])
        val mapping = assertIs<Map<*, *>>(discriminator["mapping"])
        val oneOf = assertIs<List<*>>(converted["oneOf"])

        assertEquals("type", discriminator["propertyName"])
        assertEquals("#/components/schemas/StringValue", mapping["STRING"])
        assertEquals(2, oneOf.size)
    }
}
