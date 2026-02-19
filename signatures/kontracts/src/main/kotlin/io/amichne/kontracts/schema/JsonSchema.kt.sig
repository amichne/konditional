file=kontracts/src/main/kotlin/io/amichne/kontracts/schema/JsonSchema.kt
package=io.amichne.kontracts.schema
type=io.amichne.kontracts.schema.JsonSchema|kind=class|decl=sealed class JsonSchema<out T : Any> : OpenApi<T>
fields:
- abstract override val type: OpenApi.Type
- override val title: String?
- override val description: String?
- override val default: T?
- override val nullable: Boolean
- override val example: T?
- override val deprecated: Boolean
