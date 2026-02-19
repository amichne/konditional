file=kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonValueDsl.kt
package=io.amichne.kontracts.dsl
imports=io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.value.JsonArray,io.amichne.kontracts.value.JsonBoolean,io.amichne.kontracts.value.JsonNull,io.amichne.kontracts.value.JsonNumber,io.amichne.kontracts.value.JsonObject,io.amichne.kontracts.value.JsonString,io.amichne.kontracts.value.JsonValue
type=io.amichne.kontracts.dsl.JsonValueDsl|kind=class|decl=annotation class JsonValueDsl
type=io.amichne.kontracts.dsl.JsonValueScope|kind=object|decl=object JsonValueScope
type=io.amichne.kontracts.dsl.JsonObjectBuilder|kind=class|decl=class JsonObjectBuilder
type=io.amichne.kontracts.dsl.JsonArrayBuilder|kind=class|decl=class JsonArrayBuilder
fields:
- var schema: ObjectSchema?
- var elementSchema: JsonSchema<Any>?
methods:
- fun boolean(value: Boolean): JsonBoolean
- fun string(value: String): JsonString
- fun number(value: Int): JsonNumber
- fun number(value: Double): JsonNumber
- fun nullValue(): JsonNull
- fun obj(builder: JsonObjectBuilder.() -> Unit): JsonObject
- fun array(builder: JsonArrayBuilder.() -> Unit): JsonArray
- fun field(name: String, value: JsonValue)
- fun field(name: String, builder: JsonValueScope.() -> JsonValue)
- fun fields(values: Map<String, JsonValue>)
- fun element(value: JsonValue)
- fun element(builder: JsonValueScope.() -> JsonValue)
- fun elements(values: List<JsonValue>)
