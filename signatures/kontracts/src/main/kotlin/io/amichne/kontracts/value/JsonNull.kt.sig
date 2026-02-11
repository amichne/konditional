file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNull.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.NullSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonNull|kind=object|decl=object JsonNull : JsonValue
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- override fun toString()
