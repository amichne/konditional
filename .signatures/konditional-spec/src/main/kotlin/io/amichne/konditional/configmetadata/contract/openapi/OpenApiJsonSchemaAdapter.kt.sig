file=konditional-spec/src/main/kotlin/io/amichne/konditional/configmetadata/contract/openapi/OpenApiJsonSchemaAdapter.kt
package=io.amichne.konditional.configmetadata.contract.openapi
imports=com.squareup.moshi.JsonAdapter,com.squareup.moshi.JsonDataException,com.squareup.moshi.JsonReader,com.squareup.moshi.JsonWriter,com.squareup.moshi.Moshi,com.squareup.moshi.Types,io.amichne.kontracts.schema.AllOfSchema,io.amichne.kontracts.schema.AnySchema,io.amichne.kontracts.schema.ArraySchema,io.amichne.kontracts.schema.BooleanSchema,io.amichne.kontracts.schema.DoubleSchema,io.amichne.kontracts.schema.EnumSchema,io.amichne.kontracts.schema.FieldSchema,io.amichne.kontracts.schema.IntSchema,io.amichne.kontracts.schema.JsonSchema,io.amichne.kontracts.schema.MapSchema,io.amichne.kontracts.schema.NullSchema,io.amichne.kontracts.schema.ObjectSchema,io.amichne.kontracts.schema.ObjectTraits,io.amichne.kontracts.schema.OneOfSchema,io.amichne.kontracts.schema.RefSchema,io.amichne.kontracts.schema.RootObjectSchema,io.amichne.kontracts.schema.StringSchema,java.lang.reflect.Type
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiJsonSchemaAdapterFactory|kind=object|decl=internal object OpenApiJsonSchemaAdapterFactory : JsonAdapter.Factory
type=io.amichne.konditional.configmetadata.contract.openapi.OpenApiJsonSchemaAdapter|kind=object|decl=private object OpenApiJsonSchemaAdapter : JsonAdapter<JsonSchema<*>>()
methods:
- override fun create( type: Type, annotations: Set<Annotation>, moshi: Moshi, ): JsonAdapter<*>?
- override fun toJson( writer: JsonWriter, value: JsonSchema<*>?, )
- override fun fromJson(reader: JsonReader): JsonSchema<*>
- private fun encodeSchema(schema: JsonSchema<*>): Map<String, Any?>
- private fun encodeObjectSchema( schema: JsonSchema<*>, objectTraits: ObjectTraits, ): Map<String, Any?>
- private fun encodeFieldSchema(fieldSchema: FieldSchema): Map<String, Any?>
- private fun withCommonProperties( schema: JsonSchema<*>, rawSchema: LinkedHashMap<String, Any?>, ): Map<String, Any?>
- private fun normalizeScalar(value: Any?): Any?
