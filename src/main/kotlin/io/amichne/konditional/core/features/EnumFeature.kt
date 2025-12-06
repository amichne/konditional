package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

/**
 * Feature type for user-defined enum values.
 * Provides compile-time type safety for enum-typed feature flags.
 *
 * @param E The specific enum type
 * @param M The namespace type for isolation
 */
sealed interface EnumFeature<E : Enum<E>, M : Namespace> : Feature<E, M> {
    companion object {
        internal operator fun <E : Enum<E>, M : Namespace> invoke(
            key: String,
            module: M,
        ): EnumFeature<E, M> =
            EnumFeatureImpl(key, module)

        @PublishedApi
        internal data class EnumFeatureImpl<E : Enum<E>, M : Namespace>(
            override val key: String,
            override val namespace: M,
        ) : EnumFeature<E, M>
    }
}
