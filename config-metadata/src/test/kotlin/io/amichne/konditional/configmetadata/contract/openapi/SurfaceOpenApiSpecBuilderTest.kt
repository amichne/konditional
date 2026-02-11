package io.amichne.konditional.configmetadata.contract.openapi

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SurfaceOpenApiSpecBuilderTest {
    private val generatedSpecPathProperty = "io.amichne.konditional.configmetadata.generatedOpenApiPath"

    @Test
    fun `generator writes a file to the expected build output path`() {
        val generatedPath = generatedSpecPath()

        assertTrue(
            generatedPath.endsWith("build/generated/openapi/konditional-surface-openapi.json"),
            "Generated path must use build/generated/openapi output location.",
        )
        assertTrue(Files.exists(Path.of(generatedPath)), "Generated spec file must exist during test execution.")
    }

    @Test
    fun `generated document parses as openapi json`() {
        val specDocument = parseGeneratedDocument()

        assertEquals("3.0.3", specDocument["openapi"])
        assertTrue(specDocument.containsKey("info"), "OpenAPI document must include info section.")
        assertTrue(specDocument.containsKey("paths"), "OpenAPI document must include paths section.")
        assertTrue(specDocument.containsKey("components"), "OpenAPI document must include components section.")
    }

    @Test
    fun `generated document includes canonical routes and mutation codec outcome schema`() {
        val document = parseGeneratedDocument()
        val paths = document.objectValue("paths")

        assertTrue(paths.containsKey("/v1/snapshot"), "Spec must include /v1/snapshot.")
        assertTrue(
            paths.containsKey("/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}"),
            "Spec must include canonical rule mutation path.",
        )

        val patchPath =
            paths.objectValue("/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}")
        assertTrue(patchPath.containsKey("patch"), "Canonical rule path must expose PATCH operation.")

        val snapshotPath = paths.objectValue("/v1/snapshot")
        assertTrue(snapshotPath.containsKey("get"), "Snapshot path must expose GET operation.")
        assertTrue(snapshotPath.containsKey("post"), "Snapshot path must expose POST operation.")

        val components = document.objectValue("components").objectValue("schemas")
        val mutationEnvelope = components.objectValue("MutationEnvelope")
        val mutationProperties = mutationEnvelope.objectValue("properties")
        val codecOutcomeProperty = mutationProperties.objectValue("codecOutcome")

        assertEquals(
            "#/components/schemas/CodecOutcome",
            codecOutcomeProperty["\$ref"],
            "Mutation envelope must include codecOutcome schema ref.",
        )

        val codecOutcomeSchema = components.objectValue("CodecOutcome")
        val oneOf = codecOutcomeSchema.listValue("oneOf")
        assertEquals(2, oneOf.size, "CodecOutcome must include success and failure variants.")

        val discriminator = codecOutcomeSchema.objectValue("discriminator")
        val mapping = discriminator.objectValue("mapping")

        assertEquals("#/components/schemas/CodecOutcomeSuccess", mapping["SUCCESS"])
        assertEquals("#/components/schemas/CodecOutcomeFailure", mapping["FAILURE"])

        val errorEnvelope = components.objectValue("ErrorEnvelope")
        assertTrue(
            errorEnvelope.objectValue("properties").containsKey("error"),
            "ErrorEnvelope must contain error model property.",
        )
    }

    @Test
    fun `builder output is deterministic for identical inputs`() {
        val first = SurfaceOpenApiSpecGenerator.buildJson()
        val second = SurfaceOpenApiSpecGenerator.buildJson()

        assertEquals(first, second, "Generator output must be byte-for-byte deterministic.")
        assertEquals(sha256(first), sha256(second), "Generator hash must remain stable for identical inputs.")
    }

    private fun parseGeneratedDocument(): Map<String, Any?> =
        OpenApiJsonRenderer.parse(Files.readString(Path.of(generatedSpecPath())))

    private fun generatedSpecPath(): String =
        assertNotNull(
            System.getProperty(generatedSpecPathProperty),
            "Generated spec path system property was not provided to test runtime.",
        )

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.objectValue(fieldName: String): Map<String, Any?> =
        assertNotNull(this as? Map<String, Any?>, "$fieldName must be a JSON object.")

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.objectValue(fieldName: String): Map<String, Any?> =
        assertNotNull(this[fieldName] as? Map<String, Any?>, "$fieldName must be a JSON object.")

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.listValue(fieldName: String): List<Any?> =
        assertNotNull(this[fieldName] as? List<Any?>, "$fieldName must be a JSON array.")
}
