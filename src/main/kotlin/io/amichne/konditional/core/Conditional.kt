package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Type S is constrained to EncodableValue subtypes at compile time, ensuring only
 * Boolean, String, Int, and Double can be used. This makes illegal states unrepresentable.
 *
 * @param S The EncodableValue type wrapping the actual value (Boolean, String, Int, or Double).
 * @param C The type of the context that the feature flag evaluates against.
 */
interface Conditional<S : EncodableValue<*>, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FeatureFlag<S, C>) = registry.update(definition)

    companion object {
        /**
         * Creates a Boolean Conditional.
         */
        fun <C : Context> boolean(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Conditional<EncodableValue.BooleanEncodeable, C> = create(key, registry)

        /**
         * Creates a String Conditional.
         */
        fun <C : Context> string(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Conditional<EncodableValue.StringEncodeable, C> = create(key, registry)

        /**
         * Creates an Int Conditional.
         */
        fun <C : Context> int(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Conditional<EncodableValue.IntegerEncodeable, C> = create(key, registry)

        /**
         * Creates a Double Conditional.
         */
        fun <C : Context> double(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): Conditional<EncodableValue.DecimalEncodeable, C> = create(key, registry)

        private fun <S : EncodableValue<*>, C : Context> create(
            key: String,
            registry: FlagRegistry,
        ): Conditional<S, C> = object : Conditional<S, C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        internal inline fun <reified T, S : EncodableValue<*>, C : Context> parse(key: String): T where T : Conditional<S, C>, T : Enum<T> =
            enumValues<T>().first { it.key == key }
    }
}
