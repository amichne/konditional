file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/SurfaceRouteCatalog.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=io.amichne.kontracts.dsl.BooleanSchemaBuilder,io.amichne.kontracts.dsl.StringSchemaBuilder,io.amichne.kontracts.schema.JsonSchema
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceHttpMethod|kind=enum|decl=internal enum class SurfaceHttpMethod( val wireName: String, val sortOrder: Int, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceParameterLocation|kind=enum|decl=internal enum class SurfaceParameterLocation( val wireName: String, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceParameter|kind=class|decl=internal data class SurfaceParameter( val name: String, val location: SurfaceParameterLocation, val required: Boolean, val description: String, val schema: JsonSchema<*>, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceRequestBody|kind=class|decl=internal data class SurfaceRequestBody( val componentSchema: String, val description: String, val required: Boolean = true, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceResponse|kind=class|decl=internal data class SurfaceResponse( val statusCode: Int, val description: String, val componentSchema: String?, )
type=io.amichne.konditional.configmetadata.contract.openapi.SurfaceRouteCatalog|kind=object|decl=internal object SurfaceRouteCatalog
fields:
- private val nonBlankText
- val routes: List<SurfaceRoute>
methods:
- private fun booleanSchema(default: Boolean): JsonSchema<Boolean>
- private fun pathParameter( name: String, description: String, ): SurfaceParameter
- private fun queryParameter( name: String, description: String, schema: JsonSchema<*>, required: Boolean = false, ): SurfaceParameter
