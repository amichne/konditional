package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Features are **type-bound** to their [Taxonomy], providing compile-time isolation between teams.
 * Each feature can only be defined and configured within its designated taxonomy.
 *
 * Type S is constrained to EncodableValue subtypes at compile time, ensuring type safety.
 *
 * Supports:
 * - Primitives: Boolean, String, Int, Double
 * - JSON Objects: Complex data classes and structures
 * - Custom Wrappers: Extension types that wrap primitives (DateTime, UUID, etc.)
 *
 * ## Example
 *
 * ```kotlin
 * enum class PaymentFeatures(override val key: String)
 *     : Feature<BoolEncodeable, Boolean, Context, Taxonomy.Domain.Payments> {
 *     APPLE_PAY("apple_pay");
 *     override val taxonomy = Taxonomy.Domain.Payments
 * }
 * ```
 *
 * @param S The EncodableValue type wrapping the actual value.
 * @param T The actual value type.
 * @param C The type of the context that the feature flag evaluates against.
 * @param M The taxonomy this feature belongs to (compile-time binding).
 */
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> {
    val key: String
    val module: M
    val registry: ModuleRegistry get() = module.registry

    class Impl<S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy>(
        override val key: String,
        override val module: M,
    ) : Feature<S, T, C, M>

    fun update(definition: FlagDefinition<S, T, C, M>): Unit = registry.update(definition)

    // ========== Primitive Type Interfaces ==========

    // ========== JSON Object Type Interface ==========

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
    interface OfJsonObject<T : Any, C : Context, M : Taxonomy> : Feature<EncodableValue.JsonObjectEncodeable<T>, T, C, M>

    // ========== Custom Wrapper Type Interface ==========

    /**
     * Feature for custom wrapper types that encode to primitives.
     *
     * Enables extension types: "0-depth primitive-like values" such as DateTime, UUID, etc.
     *
     * @param T The wrapper type (DateTime, UUID, etc.)
     * @param P The primitive type it encodes to (String, Int, Double, Boolean)
     * @param C The context type
     * @param M The taxonomy this feature belongs to
     */
    interface OfCustom<T : Any, P : Any, C : Context, M : Taxonomy> :
        Feature<EncodableValue.CustomEncodeable<T, P>, T, C, M>

    companion object {
        // ========== Basic Feature Factory ==========

        /**
         * Creates a basic Feature with explicit type parameters.
         *
         * Example:
         * ```kotlin
         * val ENABLED: Feature<EncodableValue.BooleanEncodeable, Boolean, Context, Taxonomy.Domain.MyTeam> =
         *     Feature("enabled", Taxonomy.Domain.MyTeam)
         * ```
         */
        operator fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> invoke(
            key: String,
            module: M,
        ): Feature<S, T, C, M> = Impl(key, module)

        // ========== JSON Object Feature Factory ==========

        /**
         * Creates a JSON Object Feature.
         *
         * Use this for complex data classes and HSON-object type representations.
         *
         * Example:
         * ```kotlin
         * data class ApiConfig(val url: String, val timeout: Int)
         *
         * val API_CONFIG: Feature.OfJsonObject<ApiConfig, Context, Taxonomy.Domain.MyTeam> =
         *     Feature.jsonObject("api_config", Taxonomy.Domain.MyTeam)
         * ```
         *
         * @param T The domain object type
         * @param C The context type
         * @param M The taxonomy this feature belongs to
         */

        fun <T : Any, C : Context, M : Taxonomy> jsonObject(
            key: String,
            module: M,
        ): OfJsonObject<T, C, M> = object : OfJsonObject<T, C, M> {
            override val key: String = key
            override val module: M = module
        }

        // ========== Custom Wrapper Feature Factory ==========

        /**
         * Creates a Custom Wrapper Feature.
         *
         * Use this for extension types that wrap JSON primitives (DateTime, UUID, etc.).
         *
         * Example:
         * ```kotlin
         * data class DateTime(val iso8601: String)
         *
         * val CREATED_AT: Feature.OfCustom<DateTime, String, Context, Taxonomy.Domain.MyTeam> =
         *     Feature.custom("created_at", Taxonomy.Domain.MyTeam)
         * ```
         *
         * @param T The wrapper type (DateTime, UUID, etc.)
         * @param P The primitive type it encodes to
         * @param C The context type
         * @param M The taxonomy this feature belongs to
         */

        fun <T : Any, P : Any, C : Context, M : Taxonomy> custom(
            key: String,
            module: M,
        ): OfCustom<T, P, C, M> = object : OfCustom<T, P, C, M> {
            override val key: String
                get() = key

            override val module: M
                get() = module
        }

        // ========== Internal ==========

        internal inline fun <reified R, S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> parse(
            key: String,
        ): R where R : Feature<S, T, C, M>, R : Enum<R> = enumValues<R>().first { it.key == key }
    }
}
