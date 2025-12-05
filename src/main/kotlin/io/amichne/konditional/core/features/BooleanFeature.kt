package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.kontext.Kontext

sealed interface BooleanFeature<C : Kontext<M>, M : Namespace> : Feature<EncodableValue<Boolean>, Boolean, C, M> {
    companion object {
        internal operator fun <C : Kontext<M>, M : Namespace> invoke(
            key: String,
            module: M,
        ): Feature<EncodableValue<Boolean>, Boolean, C, M> = BooleanFeatureImpl(key, module)

        @PublishedApi
        internal data class BooleanFeatureImpl<C : Kontext<M>, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : BooleanFeature<C, M>
    }
}
