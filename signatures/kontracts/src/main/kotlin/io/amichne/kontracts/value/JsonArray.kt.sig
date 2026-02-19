file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonArray.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ValidationResult
type=io.amichne.kontracts.value.JsonArray|kind=class|decl=data class JsonArray internal constructor( val elements: List<JsonValue>, val elementSchema: JsonSchema<Any>? = null, ) : JsonValue
fields:
- val size: Int get()
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- operator fun get(index: Int): JsonValue?
- fun isEmpty(): Boolean
- fun isNotEmpty(): Boolean
- override fun toString(): String
