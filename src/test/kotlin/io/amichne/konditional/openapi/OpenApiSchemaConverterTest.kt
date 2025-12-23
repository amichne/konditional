package io.amichne.konditional.openapi

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.rules.versions.VersionRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenApiSchemaConverterTest {
    @Test
    fun `serializable rule defaults are required and explicit`() {
        val schema = schemaFor("SerializableRule")
        val required = schema.requiredFields()

        assertEquals(
            setOf(
                "value",
                "rampUp",
                "rampUpAllowlist",
                "locales",
                "platforms",
                "versionRange",
                "axes",
            ),
            required,
        )

        val properties = schema.properties()
        val rampUpDefault = properties.property("rampUp")["default"]
        assertEquals(100.0, rampUpDefault)

        val localesDefault = properties.property("locales")["default"] as? List<*>
        assertEquals(AppLocale.entries.map { it.name }, localesDefault)

        val platformsDefault = properties.property("platforms")["default"] as? List<*>
        assertEquals(Platform.entries.map { it.name }, platformsDefault)

        val versionRangeDefault = properties.property("versionRange")["default"] as? Map<*, *>
        assertNotNull(versionRangeDefault)
        assertEquals(mapOf("type" to VersionRange.Type.UNBOUNDED.name), versionRangeDefault)
    }

    @Test
    fun `serializable flag is discriminated by default value type and enforces consistent rule value types`() {
        val schema = schemaFor("SerializableFlag")

        val options = schema.oneOfOptions()
        assertEquals(6, options.size)

        val optionTypes =
            options
                .map { option -> option.defaultValueTypeDiscriminator() to option.ruleValueTypeDiscriminator() }
                .toSet()

        assertEquals(
            setOf(
                "BOOLEAN" to "BOOLEAN",
                "STRING" to "STRING",
                "INT" to "INT",
                "DOUBLE" to "DOUBLE",
                "ENUM" to "ENUM",
                "DATA_CLASS" to "DATA_CLASS",
            ),
            optionTypes,
        )

        options.forEach { option ->
            val defaultValue = option.properties().property("defaultValue") as Map<String, Any?>
            if (defaultValue.containsKey("oneOf")) {
                error("Expected SerializableFlag.defaultValue to be a concrete FlagValue subtype schema, not oneOf.")
            }

            val rules = option.properties().property("rules") as Map<String, Any?>
            val rulesItems = requireNotNull(rules["items"] as? Map<String, Any?>) { "Expected rules.items schema." }
            val ruleValue = rulesItems.properties().property("value") as Map<String, Any?>
            if (ruleValue.containsKey("oneOf")) {
                error("Expected SerializableFlag.rules.items.value to be a concrete FlagValue subtype schema, not oneOf.")
            }
        }
    }

    private fun schemaFor(name: String): Map<String, Any?> =
        OpenApiSchemaConverter.toSchema(SerializationSchemaCatalog.schemas.getValue(name))

    private fun Map<String, Any?>.properties(): Map<*, *> =
        requireNotNull(this["properties"] as? Map<*, *>) { "Expected properties in schema." }

    private fun Map<*, *>.property(key: String): Map<*, *> =
        requireNotNull(this[key] as? Map<*, *>) { "Expected property '$key' in schema." }

    private fun Map<String, Any?>.requiredFields(): Set<String> =
        (this["required"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()

    private fun Map<String, Any?>.oneOfOptions(): List<Map<String, Any?>> =
        requireNotNull(this["oneOf"] as? List<*>) { "Expected oneOf in schema." }
            .mapIndexed { index, option ->
                requireNotNull(option as? Map<String, Any?>) { "Expected oneOf[$index] to be an object schema." }
            }

    private fun Map<String, Any?>.defaultValueTypeDiscriminator(): String {
        val defaultValueSchema = properties().property("defaultValue") as Map<String, Any?>
        val discriminatorSchema = defaultValueSchema.properties().property("type")
        val values = requireNotNull(discriminatorSchema["enum"] as? List<*>) { "Expected enum discriminator in defaultValue.type." }
        return requireNotNull(values.singleOrNull() as? String) { "Expected single enum discriminator value in defaultValue.type." }
    }

    private fun Map<String, Any?>.ruleValueTypeDiscriminator(): String {
        val rulesSchema = properties().property("rules") as Map<String, Any?>
        val itemsSchema = requireNotNull(rulesSchema["items"] as? Map<String, Any?>) { "Expected rules.items schema." }
        val valueSchema = itemsSchema.properties().property("value") as Map<String, Any?>
        val discriminatorSchema = valueSchema.properties().property("type")
        val values = requireNotNull(discriminatorSchema["enum"] as? List<*>) { "Expected enum discriminator in rules.items.value.type." }
        return requireNotNull(values.singleOrNull() as? String) { "Expected single enum discriminator value in rules.items.value.type." }
    }
}
