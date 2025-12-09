package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.json.JsonObjectEncodeable
import io.amichne.konditional.core.types.json.JsonValue

/**
 * Feature type for JSON object values.
 * Provides compile-time type safety for JSON object-typed feature flags.
 *
 * JSON objects are structured key-value pairs with a defined schema that validates
 * the structure and types at runtime while maintaining full type safety at compile time.
 *
 * @param C The context type for evaluation
 * @param M The namespace type for isolation
 */
sealed interface JsonObjectFeature<C : Context, M : Namespace> :
    Feature<JsonObjectEncodeable, JsonValue.JsonObject, C, M> {

    companion object {
        internal operator fun <C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): JsonObjectFeature<C, M> =
            JsonObjectFeatureImpl(key, module)

        @PublishedApi
        internal data class JsonObjectFeatureImpl<C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : JsonObjectFeature<C, M>
    }
}
