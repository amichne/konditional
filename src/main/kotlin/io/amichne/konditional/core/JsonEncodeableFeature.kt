package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Feature for JSON object types.
 *
 * Enables HSON-object type representation: distinct super type of object nodes
 * that represent different values given specific conditions.
 *
 * @param T The domain object type (data class, complex structure, etc.)
 * @param C The context type
 * @param M The taxonomy this feature belongs to
 */
interface JsonEncodeableFeature<T : Any, C : Context, M : Taxonomy> :
    Feature<EncodableValue.JsonObjectEncodeable<T>, T, C, M> {
    companion object {
        /**
         * Creates a JSON Object Feature.
         *
         * Use this for complex data classes and HSON-object type representations.
         *
         * Example:
         * ```kotlin
         * data class ApiConfig(val url: String, val timeout: Int)
         *
         * val API_CONFIG: Feature.JsonEncodeableFeature<ApiConfig, Context, Taxonomy.Domain.MyTeam> =
         *     Feature.jsonObject("api_config", Taxonomy.Domain.MyTeam)
         * ```
         *
         * @param T The domain object type
         * @param C The context type
         * @param M The taxonomy this feature belongs to
         */

        operator fun <T : Any, C : Context, M : Taxonomy> invoke(
            key: String,
            module: M,
            registry: ModuleRegistry = module.registry,
        ): JsonEncodeableFeature<T, C, M> = object : JsonEncodeableFeature<T, C, M> {
            override val key: String
                get() = key
            override val module: M = module
            override val registry: ModuleRegistry
                get() = registry
        }
    }
}
