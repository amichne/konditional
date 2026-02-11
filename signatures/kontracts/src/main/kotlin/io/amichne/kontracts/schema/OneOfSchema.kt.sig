file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/OneOfSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.OneOfSchema|kind=class|decl=data class OneOfSchema( val options: List<JsonSchema<*>>, val discriminator: Discriminator? = null, override val title: String? = null, override val description: String? = null, override val default: Any? = null, override val nullable: Boolean = false, override val example: Any? = null, override val deprecated: Boolean = false ) : JsonSchema<Any>()
type=io.amichne.kontracts.schema.Discriminator|kind=class|decl=data class Discriminator( val propertyName: String, val mapping: Map<String, String> )
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
