@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.konditional.serialization.SchemaValueCodec
import io.amichne.kontracts.schema.ObjectTraits

/**
 * Encodes an object-backed [Konstrained] instance to a `Map<String, Any?>` of its field values.
 *
 * This function is only valid for [Konstrained] implementations whose schema implements
 * [ObjectTraits] (i.e. [io.amichne.kontracts.schema.ObjectSchema] or
 * [io.amichne.kontracts.schema.RootObjectSchema]). For primitive/array-backed [Konstrained]
 * use [SchemaValueCodec.encodeKonstrained] instead.
 *
 * @throws IllegalArgumentException if the schema is not an object schema.
 */
@KonditionalInternalApi
fun Konstrained<*>.toPrimitiveMap(): Map<String, Any?> {
    require(schema is ObjectTraits) {
        "toPrimitiveMap() is only supported for object-backed Konstrained " +
            "(schema must implement ObjectTraits, got ${schema::class.simpleName}). " +
            "For primitive/array schemas use SchemaValueCodec.encodeKonstrained()."
    }
    return SchemaValueCodec.encode(this, schema.asObjectSchema())
        .toPrimitiveValue()
        .let { primitive ->
            require(primitive is Map<*, *>) {
                "Object-backed Konstrained must encode to a map, got ${primitive?.let { it::class.simpleName }}"
            }
            @Suppress("UNCHECKED_CAST")
            primitive as Map<String, Any?>
        }
}
