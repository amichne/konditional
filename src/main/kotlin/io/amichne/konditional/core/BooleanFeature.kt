package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.BooleanFeature.Companion.BooleanFeatureImpl
import io.amichne.konditional.core.types.EncodableValue

interface BooleanFeature<C : Context, M : FeatureModule> : Feature<EncodableValue.BooleanEncodeable, Boolean, C, M> {
    companion object {
        @PublishedApi
        internal data class BooleanFeatureImpl<C : Context, M : FeatureModule>(
            override val key: String,
            override val module: M,
        ) : BooleanFeature<C, M>
    }
}

inline fun <E, reified S : BooleanFeature<C, M>, reified C : Context, M : FeatureModule> boolean(
    key: String,
    module: M,
): S where E : Enum<E> = BooleanFeatureImpl<C, M>(key, module) as S
