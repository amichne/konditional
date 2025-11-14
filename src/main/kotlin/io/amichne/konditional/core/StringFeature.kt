package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

sealed interface StringFeature<C : Context, M : Taxonomy> : Feature<EncodableValue.StringEncodeable, String, C, M> {
    companion object {
        internal operator fun <C : Context, M : Taxonomy> invoke(key: String, module: M): StringFeature<C, M> =
            StringFeatureImpl(key, module)

        @PublishedApi
        internal data class StringFeatureImpl<C : Context, M : Taxonomy>(
            override val key: String,
            override val module: M,
        ) : StringFeature<C, M>
    }
}
