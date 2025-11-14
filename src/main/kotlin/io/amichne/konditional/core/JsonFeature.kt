package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

sealed interface JsonFeature<C : Context, M : Taxonomy, T : Any> :
    Feature<EncodableValue.JsonObjectEncodeable<T>, T, C, M> {

    companion object {
        @PublishedApi
        internal operator fun <C : Context, M : Taxonomy, T : Any> invoke(
            key: String,
            module: M,
            registry: ModuleRegistry = module.registry
        ): JsonFeature<C, M, T> = JsonFeatureImpl(key, module, registry)

        @PublishedApi
        internal data class JsonFeatureImpl<C : Context, M : Taxonomy, T : Any>(
            override val key: String,
            override val module: M,
            override val registry: ModuleRegistry,
        ) : JsonFeature<C, M, T>
    }
}
