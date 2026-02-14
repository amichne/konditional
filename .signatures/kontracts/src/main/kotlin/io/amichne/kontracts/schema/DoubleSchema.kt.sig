file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/DoubleSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.DoubleSchema|kind=class|decl=data class DoubleSchema internal constructor( override val title: String? = null, override val description: String? = null, override val default: Double? = null, override val nullable: Boolean = false, override val example: Double? = null, override val deprecated: Boolean = false, val minimum: Double? = null, val maximum: Double? = null, val enum: List<Double>? = null, val format: String? = null ) : JsonSchema<Double>()
fields:
- override val type: OpenApi.Type
methods:
- override fun toString()
