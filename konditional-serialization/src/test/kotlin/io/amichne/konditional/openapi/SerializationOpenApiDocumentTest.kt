@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.openapi

import io.amichne.konditional.api.KonditionalInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SerializationOpenApiDocumentTest {
    @Test
    fun `document exposes snapshot path and serialization component schemas`() {
        val document = SerializationOpenApiDocument.document(version = "1.2.3", title = "Schema Title")
        val info = assertIs<Map<*, *>>(document["info"])
        val paths = assertIs<Map<*, *>>(document["paths"])
        val components = assertIs<Map<*, *>>(document["components"])
        val schemas = assertIs<Map<*, *>>(components["schemas"])
        val snapshotPath = assertIs<Map<*, *>>(paths["/snapshot"])
        val getOperation = assertIs<Map<*, *>>(snapshotPath["get"])

        assertEquals("3.0.3", document["openapi"])
        assertEquals("Schema Title", info["title"])
        assertEquals("1.2.3", info["version"])
        assertEquals("Fetch a configuration snapshot", getOperation["summary"])
        assertTrue(schemas.containsKey("FeatureId"))
        assertTrue(schemas.containsKey("FlagValue"))
        assertTrue(schemas.containsKey("SerializableSnapshot"))
        assertTrue(schemas.containsKey("SerializablePatch"))
    }

    @Test
    fun `schema catalog provides strongly-typed schema accessors and component map`() {
        val schemas = SerializationSchemaCatalog.schemas

        assertTrue(schemas.serializableFlag.fields.containsKey("defaultValue"))
        assertTrue(schemas.serializableSnapshot.fields.containsKey("flags"))
        assertTrue(schemas.asMap.containsKey("SerializableFlag"))
        assertTrue(schemas.asMap.containsKey("SerializableSnapshot"))
    }
}
