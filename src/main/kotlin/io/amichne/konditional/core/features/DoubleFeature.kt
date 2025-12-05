package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.DecimalEncodeable
import io.amichne.konditional.kontext.Kontext

sealed interface DoubleFeature<C : Kontext<M>, M : Namespace> :
    Feature<DecimalEncodeable, Double, C, M> {

    companion object {
        internal operator fun <C : Kontext<M>, M : Namespace> invoke(
            key: String,
            module: M,
        ): DoubleFeature<C, M> =
            DoubleFeatureImpl(key, module)

        @PublishedApi
        internal data class DoubleFeatureImpl<C : Kontext<M>, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : DoubleFeature<C, M>
    }
}
