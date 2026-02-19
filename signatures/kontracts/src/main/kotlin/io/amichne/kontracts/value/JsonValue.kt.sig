file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonValue.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonValue|kind=interface|decl=sealed interface JsonValue
methods:
- fun validate(schema: JsonSchema<*>): ValidationResult companion object
