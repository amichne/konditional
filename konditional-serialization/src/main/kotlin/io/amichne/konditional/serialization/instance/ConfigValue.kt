@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.instance

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.serialization.SchemaValueCodec
import io.amichne.konditional.serialization.internal.toPrimitiveMap
import io.amichne.kontracts.schema.ObjectTraits

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

    /** Object-backed [Konstrained] (data classes with named fields). */
    @ConsistentCopyVisibility
    data class DataClassValue internal constructor(
        val dataClassName: String,
        val fields: Map<String, Any?>,
    ) : ConfigValue

    /**
     * Primitive/array-backed [Konstrained] (typically `@JvmInline value class`).
     *
     * [rawValue] is the underlying JSON-primitive representation:
     * `String`, `Boolean`, `Int`, `Double`, or `List<*>`.
     */
    @ConsistentCopyVisibility
    data class KonstrainedPrimitive internal constructor(
        val konstrainedClassName: String,
        val rawValue: Any,
    ) : ConfigValue

    @ConsistentCopyVisibility
    data class Opaque internal constructor(
        val typeName: String,
        val debug: String,
    ) : ConfigValue

    companion object {
        fun from(value: Any): ConfigValue =
            when (value) {
                is Boolean -> BooleanValue(value)
                is String -> StringValue(value)
                is Int -> IntValue(value)
                is Double -> DoubleValue(value)
                is Enum<*> -> EnumValue(value.javaClass.name, value.name)
                is Konstrained<*> -> fromKonstrained(value)
                else ->
                    Opaque(
                        typeName = value::class.qualifiedName ?: value::class.simpleName ?: "unknown",
                        debug = value.toString(),
                    )
            }

        private fun fromKonstrained(value: Konstrained<*>): ConfigValue =
            when {
                value.schema is ObjectTraits ->
                    DataClassValue(
                        dataClassName = value::class.java.name,
                        fields = value.toPrimitiveMap(),
                    )
                else -> {
                    val raw = SchemaValueCodec.encodeKonstrained(value).toPrimitiveRaw()
                    KonstrainedPrimitive(
                        konstrainedClassName = value::class.java.name,
                        rawValue = raw,
                    )
                }
            }
    }
}

/** Converts a [io.amichne.kontracts.value.JsonValue] to a raw primitive for [ConfigValue] storage. */
private fun io.amichne.kontracts.value.JsonValue.toPrimitiveRaw(): Any =
    when (this) {
        is io.amichne.kontracts.value.JsonBoolean -> value
        is io.amichne.kontracts.value.JsonString -> value
        is io.amichne.kontracts.value.JsonNumber ->
            toInt().let { i -> if (toDouble() == i.toDouble()) i else toDouble() }
        is io.amichne.kontracts.value.JsonArray -> elements.map { it.toPrimitiveRaw() }
        else ->
            error("Unsupported JsonValue type for ConfigValue.KonstrainedPrimitive: ${this::class.simpleName}")
    }
