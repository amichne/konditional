package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.BooleanFeature.Companion.BooleanFeatureImpl
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface BooleanFeature<C : Context> : Conditional<EncodableValue.BooleanEncodeable, Boolean, C> {
    override val registry: FlagRegistry
        get() = FlagRegistry

    companion object {
        @PublishedApi
        internal data class BooleanFeatureImpl<C : Context>(
            override val key: String,
            override val registry: FlagRegistry = FlagRegistry,
        ) : BooleanFeature<C>
    }
}

inline fun <E, reified S : BooleanFeature<C>, reified C : Context> boolean(
    key: String,
    registry: FlagRegistry = FlagRegistry,
): S where  E : Enum<E> = BooleanFeatureImpl<C>(key, registry) as S
