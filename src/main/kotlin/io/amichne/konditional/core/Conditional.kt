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
sealed interface Conditional<S : EncodableValue<T>, T : Any, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FeatureFlag<S, T, C>) = registry.update(definition)

    interface OfBoolean<C : Context> :
        Conditional<EncodableValue.BooleanEncodeable, Boolean, C>
    interface OfString<C : Context> :
        Conditional<EncodableValue.StringEncodeable, String, C>
    interface OfInt<C : Context> :
        Conditional<EncodableValue.IntegerEncodeable, Int, C>
    interface OfDouble<C : Context> :
        Conditional<EncodableValue.DecimalEncodeable, Double, C>

    companion object {
        /**
         * Creates a Boolean Conditional.
         */
        fun <C : Context> boolean(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfBoolean<C> = object : OfBoolean<C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        /**
         * Creates a String Conditional.
         */
        fun <C : Context> string(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfString<C> =  object : OfString<C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        /**
         * Creates an Int Conditional.
         */
        fun <C : Context> int(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfInt<C> = object : OfInt<C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        /**
         * Creates a Double Conditional.
         */
        fun <C : Context> double(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfDouble<C> = object : OfDouble<C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        internal inline fun <reified R, S : EncodableValue<T>, T : Any, C : Context> parse(
            key: String,
        ): R where R : Conditional<S, T, C>, R : Enum<R> =
            enumValues<R>().first { it.key == key }
    }
}
