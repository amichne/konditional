file=config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/OpenApiJsonRenderer.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.Moshi,com.squareup.moshi.Types,com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiJsonRenderer|kind=object|decl=internal object OpenApiJsonRenderer
fields:
- private val moshi: Moshi
- private val documentAdapter: JsonAdapter<OpenApiDocument>
- private val mapType
- private val mapAdapter: JsonAdapter<Map<String, Any?>>
methods:
- fun render(document: OpenApiDocument): String
- fun parse(json: String): Map<String, Any?>
