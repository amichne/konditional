file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonString.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.EnumSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.StringSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonString|kind=class|decl=data class JsonString(val value: String) : JsonValue
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- override fun toString()
