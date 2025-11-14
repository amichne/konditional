package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.FlagBuilder

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

    fun update(definition: FlagDefinition<S, T, C, M>): Unit = registry.update(definition)

    fun update(function: FlagScope<S, T, C, M>.() -> Unit): Unit =
        registry.update(FlagBuilder(this).apply(function).build())

    companion object {
        fun <T : Any, P : Any, C : Context, M : Taxonomy> custom(
            key: String,
            module: M,
        ): OfCustom<T, P, C, M> = object : OfCustom<T, P, C, M> {
            override val module: M
                get() = module
            override val key: String = key
        }
    }
}
