@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.konditional.serialization.SchemaValueCodec

@KonditionalInternalApi
fun Konstrained<*>.toPrimitiveMap(): Map<String, Any?> =
    SchemaValueCodec.encode(this, schema.asObjectSchema())
        .toPrimitiveValue()
        .let { primitive ->
            require(primitive is Map<*, *>) {
                "Konstrained must encode to an object, got ${primitive?.let { it::class.simpleName }}"
            }
            @Suppress("UNCHECKED_CAST")
            primitive as Map<String, Any?>
        }
