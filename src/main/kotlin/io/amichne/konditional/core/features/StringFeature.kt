package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.StringEncodeable
import io.amichne.konditional.kontext.Kontext

sealed interface StringFeature<C : Kontext<M>, M : Namespace> : Feature<StringEncodeable, String, C, M> {
    companion object {
        internal operator fun <C : Kontext<M>, M : Namespace> invoke(
            key: String,
            module: M,
        ): StringFeature<C, M> = StringFeatureImpl(key, module)

        @PublishedApi
        internal data class StringFeatureImpl<C : Kontext<M>, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : StringFeature<C, M>
    }
}
