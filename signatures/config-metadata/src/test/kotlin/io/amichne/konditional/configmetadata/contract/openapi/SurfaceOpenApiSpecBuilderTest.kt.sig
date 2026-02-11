file=config-metadata/src/test/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceOpenApiSpecBuilderTest.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=java.nio.file.Files,java.nio.file.Path,java.security.MessageDigest,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertNotNull,kotlin.test.assertTrue
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceOpenApiSpecBuilderTest|kind=class|decl=class SurfaceOpenApiSpecBuilderTest
fields:
- private val generatedSpecPathProperty
methods:
- fun `generator writes a file to the expected build output path`()
- fun `generated document parses as openapi json`()
- fun `generated document includes canonical routes and mutation codec outcome schema`()
- fun `builder output is deterministic for identical inputs`()
- private fun parseGeneratedDocument(): Map<String, Any?>
- private fun generatedSpecPath(): String
- private fun sha256(value: String): String
- private fun Any?.objectValue(fieldName: String): Map<String, Any?>
- private fun Map<String, Any?>.objectValue(fieldName: String): Map<String, Any?>
- private fun Map<String, Any?>.listValue(fieldName: String): List<Any?>
