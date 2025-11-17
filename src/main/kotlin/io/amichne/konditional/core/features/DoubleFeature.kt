package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.registry.ModuleRegistry
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

sealed interface DoubleFeature<C : Context, M : Taxonomy> :
    Feature<EncodableValue.DecimalEncodeable, Double, C, M> {

    companion object {
        internal operator fun <C : Context, M : Taxonomy> invoke(key: String, module: M,
                                                                 registry: ModuleRegistry = module.registry
                                                                 ): DoubleFeature<C, M> =
            DoubleFeatureImpl(key, module, registry)

        @PublishedApi
        internal data class DoubleFeatureImpl<C : Context, M : Taxonomy>(
            override val key: String,
            override val module: M,
            override val registry: ModuleRegistry,
        ) : DoubleFeature<C, M>
    }
}
