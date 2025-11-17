package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.registry.ModuleRegistry
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

sealed interface StringFeature<C : Context, M : Taxonomy> : Feature<EncodableValue.StringEncodeable, String, C, M> {
    companion object {
        internal operator fun <C : Context, M : Taxonomy> invoke(
            key: String,
            module: M,
            registry: ModuleRegistry = module.registry,
        ): StringFeature<C, M> =
            StringFeatureImpl(key, module, registry)

        @PublishedApi
        internal data class StringFeatureImpl<C : Context, M : Taxonomy>(
            override val key: String,
            override val module: M,
            override val registry: ModuleRegistry = module.registry,
        ) : StringFeature<C, M>
    }
}
