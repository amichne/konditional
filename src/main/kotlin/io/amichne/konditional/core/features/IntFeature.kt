package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

sealed interface IntFeature<M : Namespace> : Feature<Int, M> {
    companion object {
        internal operator fun <M : Namespace> invoke(
            key: String,
            module: M,
        ): IntFeature<M> =
            IntFeatureImpl(key, module)

        @PublishedApi
        internal data class IntFeatureImpl<M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : IntFeature<M>
    }
}
