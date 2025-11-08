package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface StringFeature<C : Context> : Conditional<EncodableValue.StringEncodeable, String, C> {
    override val registry: FlagRegistry
        get() = FlagRegistry

    companion object {
        @PublishedApi
        internal data class StringFeatureImpl<C : Context>(
            override val key: String,
            override val registry: FlagRegistry = FlagRegistry,
        ) : StringFeature<C>
    }
}

inline fun <reified C : Context,  E, reified S : StringFeature<C>> string(
    key: String,
    registry: FlagRegistry = FlagRegistry,
): S where E : Enum<E> = StringFeature.Companion.StringFeatureImpl<C>(key, registry) as S
