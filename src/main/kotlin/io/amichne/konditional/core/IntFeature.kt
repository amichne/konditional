package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

@SubclassOptInRequired
interface IntFeature<C : Context, M : FeatureModule> : Feature<EncodableValue.IntEncodeable, Int, C, M> {

    companion object {
        @PublishedApi
        internal data class IntFeatureImpl<C : Context, M : FeatureModule>(
            override val key: String,
            override val module: M,
        ) : IntFeature<C, M>
    }
}

inline fun <reified C : Context, E, reified S : IntFeature<C, M>, M : FeatureModule> int(
    key: String,
    module: M,
): S where E : Enum<E> = IntFeature.Companion.IntFeatureImpl<C, M>(key, module) as S
