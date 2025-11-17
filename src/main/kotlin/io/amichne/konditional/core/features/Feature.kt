package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Features are **type-bound** to their [io.amichne.konditional.core.Taxonomy], providing compile-time isolation between teams.
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
}
