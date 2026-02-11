file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/StringSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.StringSchema|kind=class|decl=data class StringSchema( override val title: String? = null, override val description: String? = null, override val default: String? = null, override val nullable: Boolean = false, override val example: String? = null, override val deprecated: Boolean = false, val minLength: Int? = null, val maxLength: Int? = null, val pattern: String? = null, val format: String? = null, val enum: List<String>? = null ) : JsonSchema<String>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
