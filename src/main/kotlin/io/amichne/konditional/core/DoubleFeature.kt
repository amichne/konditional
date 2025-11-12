package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface DoubleFeature<C : Context, M : FeatureModule> : Feature<EncodableValue.DecimalEncodeable, Double, C, M> {

    companion object {
        @PublishedApi
        internal data class DoubleFeatureImpl<C : Context, M : FeatureModule>(
            override val key: String,
            override val module: M,
        ) : DoubleFeature<C, M>
    }
}

inline fun <reified C : Context, E, reified S : DoubleFeature<C, M>, M : FeatureModule> double(
    key: String,
    module: M,
): S where E : Enum<E> = DoubleFeature.Companion.DoubleFeatureImpl<C, M>(key, module) as S
