file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.ObjectSchema|kind=class|decl=data class ObjectSchema( override val fields: Map<String, FieldSchema>, override val title: String? = null, override val description: String? = null, override val default: Map<String, Any?>? = null, override val nullable: Boolean = false, override val example: Map<String, Any?>? = null, override val deprecated: Boolean = false, override val required: Set<String>? = null ) : JsonSchema<Map<String, Any?>>(), ObjectTraits
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
