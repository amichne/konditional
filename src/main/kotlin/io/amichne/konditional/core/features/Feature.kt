package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Features are **type-bound** to their [io.amichne.konditional.core.Namespace], providing compile-time isolation between teams.
 * Each feature can only be defined and configured within its designated namespace.
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
 *     : Feature<BoolEncodeable, Boolean, Context, Namespace.Payments> {
 *     APPLE_PAY("apple_pay");
 *     override val namespace = Namespace.Payments
 * }
 * ```
 *
 * @param S The EncodableValue type wrapping the actual value.
 * @param T The actual value type.
 * @param C The type of the context that the feature flag evaluates against.
 * @param M The namespace this feature belongs to (compile-time binding).
 */
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace> {
    val key: String
    val namespace: M
}
