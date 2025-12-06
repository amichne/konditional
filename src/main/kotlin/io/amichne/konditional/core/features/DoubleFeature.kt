package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

sealed interface DoubleFeature<M : Namespace> : Feature<Double, M> {
    companion object {
        internal operator fun <M : Namespace> invoke(
            key: String,
            module: M,
        ): DoubleFeature<M> =
            DoubleFeatureImpl(key, module)

        @PublishedApi
        internal data class DoubleFeatureImpl<M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : DoubleFeature<M>
    }
}
