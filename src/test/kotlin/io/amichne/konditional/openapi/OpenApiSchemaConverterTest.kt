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

    private fun schemaFor(name: String): Map<String, Any?> =
        OpenApiSchemaConverter.toSchema(SerializationSchemaCatalog.schemas.getValue(name))

    private fun Map<String, Any?>.properties(): Map<*, *> =
        requireNotNull(this["properties"] as? Map<*, *>) { "Expected properties in schema." }

    private fun Map<*, *>.property(key: String): Map<*, *> =
        requireNotNull(this[key] as? Map<*, *>) { "Expected property '$key' in schema." }

    private fun Map<String, Any?>.requiredFields(): Set<String> =
        (this["required"] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
}
