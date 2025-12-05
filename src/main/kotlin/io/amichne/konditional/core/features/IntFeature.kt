package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.IntEncodeable
import io.amichne.konditional.kontext.Kontext

sealed interface IntFeature<C : Kontext<M>, M : Namespace> : Feature<IntEncodeable, Int, C, M> {
    companion object {
        internal operator fun <C : Kontext<M>, M : Namespace> invoke(
            key: String,
            module: M,
        ): IntFeature<C, M> =
            IntFeatureImpl(key, module)

        @PublishedApi
        internal data class IntFeatureImpl<C : Kontext<M>, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : IntFeature<C, M>
    }
}
