file=konditional-spec/src/test/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiSpecBuilderTest.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=java.nio.file.Files,java.nio.file.Path,java.security.MessageDigest,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertNotNull,kotlin.test.assertTrue
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceOpenApiSpecBuilderTest|kind=class|decl=class SurfaceOpenApiSpecBuilderTest
fields:
- private val generatedSpecPathProperty
methods:
- fun `generator writes a file to the expected build output path`()
- fun `generated document parses as openapi json`()
- fun `generated document includes full route surface with stable operation ids`()
- fun `profile gating controls legacy post snapshot route exposure`()
- fun `generated document encodes codec discriminator and phase enums`()
- fun `builder output is deterministic for identical inputs`()
- private fun parseGeneratedDocument(): Map<String, Any?>
- private fun parseJson(json: String): Map<String, Any?>
- private fun generatedSpecPath(): String
- private fun assertOperation( document: Map<String, Any?>, method: String, path: String, expectedOperationId: String, )
- private fun operationMap( document: Map<String, Any?>, path: String, ): Map<String, Any?>
- private fun sha256(value: String): String
- private fun Any?.objectValue(fieldName: String): Map<String, Any?>
- private fun Map<String, Any?>.objectValue(fieldName: String): Map<String, Any?>
- private fun Map<String, Any?>.listValue(fieldName: String): List<Any?>
- private fun Map<String, Any?>.stringListValue(fieldName: String): List<String>
