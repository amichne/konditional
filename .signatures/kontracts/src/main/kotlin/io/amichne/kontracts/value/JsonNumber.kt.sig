file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNumber.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonNumber|kind=class|decl=data class JsonNumber internal constructor(val value: Double) : JsonValue
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- fun toInt(): Int
- fun toDouble(): Double
- override fun toString()
