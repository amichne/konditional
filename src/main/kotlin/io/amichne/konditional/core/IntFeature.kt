package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

sealed interface IntFeature<C : Context, M : Taxonomy> : Feature<EncodableValue.IntEncodeable, Int, C, M> {
    companion object {
        internal operator fun <C : Context, M : Taxonomy> invoke(key: String, module: M): IntFeature<C, M> =
            IntFeatureImpl(key, module)

        @PublishedApi
        internal data class IntFeatureImpl<C : Context, M : Taxonomy>(
            override val key: String,
            override val module: M,
        ) : IntFeature<C, M>
    }
}
