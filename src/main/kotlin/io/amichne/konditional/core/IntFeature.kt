package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface IntFeature<C : Context> : Conditional<EncodableValue.IntEncodeable, Int, C> {
    override val registry: FlagRegistry
        get() = FlagRegistry

    companion object {
        @PublishedApi
        internal data class IntFeatureImpl<C : Context>(
            override val key: String,
            override val registry: FlagRegistry = FlagRegistry,
        ) : IntFeature<C>
    }
}

inline fun <reified C : Context, E, reified S : IntFeature<C>> int(
    key: String,
    registry: FlagRegistry = FlagRegistry,
): S where E : Enum<E> = IntFeature.Companion.IntFeatureImpl<C>(key, registry) as S
