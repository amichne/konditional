package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.ModuleRegistry
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

sealed interface BooleanFeature<C : Context, M : Taxonomy> : Feature<EncodableValue.BooleanEncodeable, Boolean, C, M> {
    companion object {
        internal operator fun <C : Context, M : Taxonomy> invoke(
            key: String,
            module: M,
            registry: ModuleRegistry = module.registry,
        ): BooleanFeature<C, M> =
            BooleanFeatureImpl(key, module, registry)

        @PublishedApi
        internal data class BooleanFeatureImpl<C : Context, M : Taxonomy>(
            override val key: String,
            override val module: M,
            override val registry: ModuleRegistry = module.registry,
        ) : BooleanFeature<C, M>
    }
}
