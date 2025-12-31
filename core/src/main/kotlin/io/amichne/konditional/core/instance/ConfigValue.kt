package io.amichne.konditional.core.instance

import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.core.types.toPrimitiveValue

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
                // Use registry-based serializer (no reflection)
                val serializer = io.amichne.konditional.serialization.SerializerRegistry.get(value::class)
                    ?: throw IllegalArgumentException(
                        "No serializer registered for ${value::class.qualifiedName}. " +
                            "Register with SerializerRegistry.register(${value::class.simpleName}::class, serializer)"
                    )

                @Suppress("UNCHECKED_CAST")
                val json =
                    (serializer as io.amichne.konditional.serialization.TypeSerializer<KotlinEncodeable<*>>).encode(
                        value
                    )

                val primitive = json.toPrimitiveValue()
                require(primitive is Map<*, *>) { "KotlinEncodeable must encode to an object, got ${primitive?.let { it::class.simpleName }}" }
                DataClassValue(
                    dataClassName = value::class.java.name,
                    fields =
                        @Suppress("UNCHECKED_CAST")
                        (primitive as Map<String, Any?>),
                )
            }
            else -> Opaque(
                typeName = value::class.qualifiedName ?: value::class.simpleName ?: "unknown",
                debug = value.toString(),
            )
        }
    }
}
