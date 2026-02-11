file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonBoolean.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonBoolean|kind=class|decl=data class JsonBoolean(val value: Boolean) : JsonValue
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- override fun toString()
