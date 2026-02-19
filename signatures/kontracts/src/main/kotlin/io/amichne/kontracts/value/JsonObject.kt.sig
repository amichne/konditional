file=kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonObject.kt
package=io.amichne.kontracts.value
imports=io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ValidationResult,io.amichne.kontracts.schema.ValidationResult.Invalid
type=io.amichne.kontracts.value.JsonObject|kind=class|decl=data class JsonObject internal constructor( val fields: Map<String, JsonValue>, val schema: ObjectSchema? = null, ) : JsonValue
methods:
- override fun validate(schema: JsonSchema<*>): ValidationResult
- operator fun get(key: String): JsonValue?
- inline fun <reified T> getTyped(key: String): T?
- override fun toString(): String
