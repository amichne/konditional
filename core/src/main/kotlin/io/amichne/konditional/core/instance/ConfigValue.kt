package io.amichne.konditional.core.instance

import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.konditional.core.types.toPrimitiveValue
import io.amichne.konditional.serialization.SchemaValueCodec

sealed interface ConfigValue {
    @ConsistentCopyVisibility
    data class BooleanValue internal constructor(val value: Boolean) : ConfigValue

    @ConsistentCopyVisibility
    data class StringValue internal constructor(val value: String) : ConfigValue

    @ConsistentCopyVisibility
    data class IntValue internal constructor(val value: Int) : ConfigValue

    @ConsistentCopyVisibility
    data class DoubleValue internal constructor(val value: Double) : ConfigValue

    @ConsistentCopyVisibility
    data class EnumValue internal constructor(
        val enumClassName: String,
        val constantName: String,
    ) : ConfigValue

    @ConsistentCopyVisibility
    data class DataClassValue internal constructor(
        val dataClassName: String,
        val fields: Map<String, Any?>,
    ) : ConfigValue

    @ConsistentCopyVisibility
    data class Opaque internal constructor(
        val typeName: String,
        val debug: String,
    ) : ConfigValue

    companion object {
        fun from(value: Any): ConfigValue = when (value) {
            is Boolean -> BooleanValue(value)
            is String -> StringValue(value)
            is Int -> IntValue(value)
            is Double -> DoubleValue(value)
            is Enum<*> -> EnumValue(value.javaClass.name, value.name)
            is KotlinEncodeable<*> -> {
                // Use schema-based serializer
                val schema = value.schema.asObjectSchema()
                val json = SchemaValueCodec.encode(value, schema)
                val primitive = json.toPrimitiveValue()

                require(primitive is Map<*, *>) {
                    "KotlinEncodeable must encode to an object, got ${primitive?.let { it::class.simpleName }}"
                }

                DataClassValue(
                    dataClassName = value::class.java.name,
                    fields = @Suppress("UNCHECKED_CAST") (primitive as Map<String, Any?>),
                )
            }
            else -> Opaque(
                typeName = value::class.qualifiedName ?: value::class.simpleName ?: "unknown",
                debug = value.toString(),
            )
        }
    }
}
