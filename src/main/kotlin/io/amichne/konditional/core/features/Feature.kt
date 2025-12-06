package io.amichne.konditional.core.features

import io.amichne.konditional.core.Namespace

/**
 * Represents a feature flag that can be used to enable or disable specific functionality
 * in an application based on a given state or condition.
 *
 * Features are **type-bound** to their [io.amichne.konditional.core.Namespace], providing compile-time isolation between teams.
 * Each feature can only be defined and configured within its designated namespace.
 *
 * Supports:
 * - Primitives: Boolean, String, Int, Double
 * - Enums: Type-safe enum values
 * - Data Classes: Structured configuration with schema validation
 * - JSON Objects: Complex data structures
 *
 * ## Example
 *
 * ```kotlin
 * object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
 *     val APPLE_PAY by boolean(default = false) {
 *         rule { platforms(Platform.IOS) } returns true
 *     }
 * }
 * ```
 *
 * @param T The value type (Boolean, String, Int, Double, Enum, DataClass).
 * @param M The namespace this feature belongs to (compile-time binding).
 */
sealed interface Feature<T : Any, M : Namespace> {
    val key: String
    val namespace: M
}
