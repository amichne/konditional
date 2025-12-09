package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.json.JsonArrayEncodeable
import io.amichne.konditional.core.types.json.JsonValue

/**
 * Feature type for JSON array values.
 * Provides compile-time type safety for JSON array-typed feature flags.
 *
 * JSON arrays are homogeneous lists of values with a defined element schema that validates
 * all elements match the expected type at runtime while maintaining full type safety at compile time.
 *
 * @param C The context type for evaluation
 * @param M The namespace type for isolation
 */
sealed interface JsonArrayFeature<C : Context, M : Namespace> :
    Feature<JsonArrayEncodeable, JsonValue.JsonArray, C, M> {

    companion object {
        internal operator fun <C : Context, M : Namespace> invoke(
            key: String,
            module: M,
        ): JsonArrayFeature<C, M> =
            JsonArrayFeatureImpl(key, module)

        @PublishedApi
        internal data class JsonArrayFeatureImpl<C : Context, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : JsonArrayFeature<C, M>
    }
}
