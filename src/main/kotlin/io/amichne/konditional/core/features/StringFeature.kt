package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

sealed interface StringFeature<M : Namespace> : Feature<String, M> {
    companion object {
        internal operator fun <M : Namespace> invoke(
            key: String,
            module: M,
        ): StringFeature<M> =
            StringFeatureImpl(key, module)

        @PublishedApi
        internal data class StringFeatureImpl<M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : StringFeature<M>
    }
}
