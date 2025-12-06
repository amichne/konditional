package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

sealed interface BooleanFeature<M : Namespace> : Feature<Boolean, M> {
    companion object {
        internal operator fun <M : Namespace> invoke(
            key: String,
            module: M,
        ): BooleanFeature<M> =
            BooleanFeatureImpl(key, module)

        @PublishedApi
        internal data class BooleanFeatureImpl<M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : BooleanFeature<M>
    }
}
