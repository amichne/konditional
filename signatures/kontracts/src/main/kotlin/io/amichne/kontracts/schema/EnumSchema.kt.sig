file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/EnumSchema.kt
package=io.amichne.kontracts.schema
imports=kotlin.reflect.KClass
type=io.amichne.kontracts.schema.EnumSchema|kind=class|decl=data class EnumSchema<E : Enum<E>> internal constructor( val enumClass: KClass<E>, val values: List<E>, override val title: String? = null, override val description: String? = null, override val default: E? = null, override val nullable: Boolean = false, override val example: E? = null, override val deprecated: Boolean = false ) : JsonSchema<E>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
