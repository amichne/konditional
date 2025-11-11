package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Type S is constrained to EncodableValue subtypes at compile time, ensuring type safety.
 *
 * Supports:
 * - Primitives: Boolean, String, Int, Double
 * - JSON Objects: Complex data classes and structures
 * - Custom Wrappers: Extension types that wrap primitives (DateTime, UUID, etc.)
 *
 * @param S The EncodableValue type wrapping the actual value.
 * @param T The actual value type.
 * @param C The type of the context that the feature flag evaluates against.
 */
sealed interface Conditional<S : EncodableValue<T>, T : Any, C : Context> {
    val registry: FlagRegistry
    val key: String

    fun update(definition: FlagDefinition<S, T, C>): Unit = registry.update(definition)

    // ========== Primitive Type Interfaces ==========

    // ========== JSON Object Type Interface ==========

    /**
     * Conditional for JSON object types.
     *
     * Enables HSON-object type representation: distinct super type of object nodes
     * that represent different values given specific conditions.
     *
     * @param T The domain object type (data class, complex structure, etc.)
     * @param C The context type
     */
    interface OfJsonObject<T : Any, C : Context> :
        Conditional<EncodableValue.JsonObjectEncodeable<T>, T, C>

    // ========== Custom Wrapper Type Interface ==========

    /**
     * Conditional for custom wrapper types that encode to primitives.
     *
     * Enables extension types: "0-depth primitive-like values" such as DateTime, UUID, etc.
     *
     * @param T The wrapper type (DateTime, UUID, etc.)
     * @param P The primitive type it encodes to (String, Int, Double, Boolean)
     * @param C The context type
     */
    interface OfCustom<T : Any, P : Any, C : Context> :
        Conditional<EncodableValue.CustomEncodeable<T, P>, T, C>

    companion object {
        // ========== JSON Object Conditional Factory ==========

        /**
         * Creates a JSON Object Conditional.
         *
         * Use this for complex data classes and HSON-object type representations.
         *
         * Example:
         * ```kotlin
         * data class ApiConfig(val url: String, val timeout: Int)
         *
         * val API_CONFIG: Conditional.OfJsonObject<ApiConfig, Context> =
         *     Conditional.jsonObject("api_config")
         * ```
         *
         * @param T The domain object type
         * @param C The context type
         */
        fun <T : Any, C : Context> jsonObject(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfJsonObject<T, C> = object : OfJsonObject<T, C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        // ========== Custom Wrapper Conditional Factory ==========

        /**
         * Creates a Custom Wrapper Conditional.
         *
         * Use this for extension types that wrap JSON primitives (DateTime, UUID, etc.).
         *
         * Example:
         * ```kotlin
         * data class DateTime(val iso8601: String)
         *
         * val CREATED_AT: Conditional.OfCustom<DateTime, String, Context> =
         *     Conditional.custom("created_at")
         * ```
         *
         * @param T The wrapper type (DateTime, UUID, etc.)
         * @param P The primitive type it encodes to
         * @param C The context type
         */
        fun <T : Any, P : Any, C : Context> custom(
            key: String,
            registry: FlagRegistry = FlagRegistry,
        ): OfCustom<T, P, C> = object : OfCustom<T, P, C> {
            override val registry: FlagRegistry = registry
            override val key: String = key
        }

        // ========== Internal ==========

        internal inline fun <reified R, S : EncodableValue<T>, T : Any, C : Context> parse(
            key: String,
        ): R where R : Conditional<S, T, C>, R : Enum<R> =
            enumValues<R>().first { it.key == key }
    }
}
