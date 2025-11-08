package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface DoubleFeature<C : Context> : Conditional<EncodableValue.DecimalEncodeable, Double, C> {
    override val registry: FlagRegistry
        get() = FlagRegistry

    companion object {
        @PublishedApi
        internal data class DoubleFeatureImpl<C : Context>(
            override val key: String,
            override val registry: FlagRegistry = FlagRegistry,
        ) : DoubleFeature<C>
    }
}

inline fun <reified C : Context, E, reified S : DoubleFeature<C>> double(
    key: String,
    registry: FlagRegistry = FlagRegistry,
): S where E : Enum<E> = DoubleFeature.Companion.DoubleFeatureImpl<C>(key, registry) as S
