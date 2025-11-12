package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface StringFeature<C : Context, M : FeatureModule> : Feature<EncodableValue.StringEncodeable, String, C, M> {

    companion object {
        @PublishedApi
        internal data class StringFeatureImpl<C : Context, M : FeatureModule>(
            override val key: String,
            override val module: M,
        ) : StringFeature<C, M>
    }
}

inline fun <reified C : Context, E, reified S : StringFeature<C, M>, M : FeatureModule> string(
    key: String,
    module: M,
): S where E : Enum<E> = StringFeature.Companion.StringFeatureImpl<C, M>(key, module) as S
