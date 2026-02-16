package io.amichne.konditional.server.core.openapi

import io.amichne.konditional.server.core.surface.profile.SurfaceProfile
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SurfaceOpenApiSpecBuilderTest {
    private val generatedSpecPathProperty = "io.amichne.konditional.spec.generatedOpenApiPath"

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
    fun `generated document includes full route surface with stable operation ids`() {
        val document = parseGeneratedDocument()

        assertOperation(document, "GET", "/v1/snapshot", "getSnapshotV1")
        assertOperation(document, "POST", "/v1/snapshot", "mutateSnapshotV1")
        assertOperation(document, "PATCH", "/v1/snapshot", "patchSnapshotV1")

        assertOperation(document, "GET", "/v1/namespaces/{namespaceId}/snapshot", "getNamespaceSnapshotV1")
        assertOperation(document, "PATCH", "/v1/namespaces/{namespaceId}", "patchNamespaceV1")

        assertOperation(document, "POST", "/v1/namespaces/{namespaceId}/features", "createFeatureV1")
        assertOperation(document, "GET", "/v1/namespaces/{namespaceId}/features/{featureKey}", "getFeatureV1")
        assertOperation(document, "PATCH", "/v1/namespaces/{namespaceId}/features/{featureKey}", "patchFeatureV1")

        assertOperation(
            document,
            "GET",
            "/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}",
            "getRuleV1",
        )
        assertOperation(
            document,
            "PATCH",
            "/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}",
            "patchRuleV1",
        )
    }

    @Test
    fun `profile gating controls legacy post snapshot route exposure`() {
        val devDocument = parseJson(SurfaceOpenApiSpecBuilder(profile = SurfaceProfile.DEV).buildJson())
        val qaDocument = parseJson(SurfaceOpenApiSpecBuilder(profile = SurfaceProfile.QA).buildJson())
        val prodDocument = parseJson(SurfaceOpenApiSpecBuilder(profile = SurfaceProfile.PROD).buildJson())

        assertOperation(devDocument, "POST", "/v1/snapshot", "mutateSnapshotV1")
        assertOperation(qaDocument, "POST", "/v1/snapshot", "mutateSnapshotV1")

        assertFalse(
            operationMap(prodDocument, "/v1/snapshot").containsKey("post"),
            "Prod profile must not expose legacy POST snapshot mutation route.",
        )
        assertOperation(prodDocument, "PATCH", "/v1/snapshot", "patchSnapshotV1")
    }

    @Test
    fun `generated document encodes codec discriminator and phase enums`() {
        val document = parseGeneratedDocument()
        val components = document.objectValue("components").objectValue("schemas")

        val codecOutcomeSchema = components.objectValue("CodecOutcome")
        val oneOf = codecOutcomeSchema.listValue("oneOf")
        assertEquals(2, oneOf.size, "CodecOutcome must include success and failure variants.")

        val discriminator = codecOutcomeSchema.objectValue("discriminator")
        val mapping = discriminator.objectValue("mapping")
        assertEquals("#/components/schemas/CodecOutcomeSuccess", mapping["SUCCESS"])
        assertEquals("#/components/schemas/CodecOutcomeFailure", mapping["FAILURE"])

        val codecPhaseSchema = components.objectValue("CodecPhase")
        assertEquals(
            listOf("DECODE_REQUEST", "APPLY_MUTATION", "ENCODE_RESPONSE"),
            codecPhaseSchema.stringListValue("enum"),
            "CodecPhase enum values must remain stable.",
        )

        val codecOutcomeSuccessSchema = components.objectValue("CodecOutcomeSuccess")
        val successStatus =
            codecOutcomeSuccessSchema
                .objectValue("properties")
                .objectValue("status")
                .stringListValue("enum")
        assertEquals(listOf("SUCCESS"), successStatus)

        val codecOutcomeFailureSchema = components.objectValue("CodecOutcomeFailure")
        val failureStatus =
            codecOutcomeFailureSchema
                .objectValue("properties")
                .objectValue("status")
                .stringListValue("enum")
        assertEquals(listOf("FAILURE"), failureStatus)
    }

    @Test
    fun `builder output is deterministic for identical inputs`() {
        val first = SurfaceOpenApiSpecGenerator.buildJson()
        val second = SurfaceOpenApiSpecGenerator.buildJson()

        assertEquals(first, second, "Generator output must be byte-for-byte deterministic.")
        assertEquals(sha256(first), sha256(second), "Generator hash must remain stable for identical inputs.")
    }

    private fun parseGeneratedDocument(): Map<String, Any?> =
        parseJson(Files.readString(Path.of(generatedSpecPath())))

    private fun parseJson(json: String): Map<String, Any?> = OpenApiJsonRenderer.parse(json)

    private fun generatedSpecPath(): String =
        assertNotNull(
            System.getProperty(generatedSpecPathProperty),
            "Generated spec path system property was not provided to test runtime.",
        )

    private fun assertOperation(
        document: Map<String, Any?>,
        method: String,
        path: String,
        expectedOperationId: String,
    ) {
        val operation = operationMap(document, path).objectValue(method.lowercase())
        assertEquals(expectedOperationId, operation["operationId"], "Unexpected operationId for $method $path")
        assertTrue(
            operation.containsKey("responses"),
            "Operation $method $path must include responses block for OpenAPI parseability.",
        )
    }

    private fun operationMap(
        document: Map<String, Any?>,
        path: String,
    ): Map<String, Any?> {
        val paths = document.objectValue("paths")
        return paths.objectValue(path)
    }

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

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.stringListValue(fieldName: String): List<String> =
        assertNotNull(this[fieldName] as? List<String>, "$fieldName must be a JSON string array.")
}
