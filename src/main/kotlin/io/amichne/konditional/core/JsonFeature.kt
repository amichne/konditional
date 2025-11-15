package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Feature for JSON object types.
 *
 * Enables JSON-object type representation: distinct super type of object nodes
 * that represent different values given specific conditions.
 *
 * @param C The context type
 * @param M The taxonomy this feature belongs to
 * @param T The domain object type (data class, complex structure, etc.)
 */
sealed interface JsonFeature<C : Context, M : Taxonomy, T : Any> :
    Feature<EncodableValue.JsonObjectEncodeable<T>, T, C, M> {

    companion object {
        /**
         * Creates a JSON Object Feature.
         *
         * Use this for complex data classes and JSON-object type representations.
         *
         * Example:
         * ```kotlin
         * data class ApiConfig(val url: String, val timeout: Int)
         *
         * val API_CONFIG: JsonFeature<Context, Taxonomy.Domain.MyTeam, ApiConfig> =
         *     JsonFeature("api_config", Taxonomy.Domain.MyTeam)
         * ```
         *
         * @param C The context type
         * @param M The taxonomy this feature belongs to
         * @param T The domain object type
         */
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
