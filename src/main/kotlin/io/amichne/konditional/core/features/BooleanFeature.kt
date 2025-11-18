package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue

sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<EncodableValue.BooleanEncodeable, Boolean, C, M> {
    companion object {
        internal operator fun <C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): BooleanFeature<C, M> =
            BooleanFeatureImpl(key, module)

        @PublishedApi
        internal data class BooleanFeatureImpl<C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : BooleanFeature<C, M>
    }
}
