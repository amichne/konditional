package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.IntEncodeable

sealed interface IntFeature<C : Context, M : Namespace> : Feature<IntEncodeable, Int, C, M> {
    companion object {
        internal operator fun <C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): IntFeature<C, M> = IntFeatureImpl(key, module)

        @PublishedApi
        internal data class IntFeatureImpl<C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : IntFeature<C, M>
    }
}
