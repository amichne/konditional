package io.amichne.konditional.core.dsl.json

import io.amichne.konditional.core.dsl.TypedFieldScope
import io.amichne.konditional.core.types.json.JsonSchema
import kotlin.reflect.KProperty0

@PublishedApi
internal class TypedFieldBuilder<V : Any>(
    val property: KProperty0<V>,
    val schema: JsonSchema
) : TypedFieldScope<V> {
    val isNullable: Boolean = property.returnType.isMarkedNullable
    var default: V? = null

    override fun default(value: V) {
        default = value
    }
}
