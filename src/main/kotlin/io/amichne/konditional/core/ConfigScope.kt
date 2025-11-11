package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.types.EncodableValue

/**
 * DSL scope for configuration building.
 *
 * This interface defines the public API for configuring feature flags. Users cannot
 * instantiate implementations of this interface directly - it is only available as a
 * receiver in DSL blocks through internal implementations.
 *
 * Example usage:
 * ```kotlin
 * config {
 *     MyFeature.FEATURE_A with {
 *         default(true)
 *         rule { platforms(Platform.IOS) }.implies(false)
 *     }
 * }
 * ```
 *
 * @since 0.0.2
 */
@FeatureFlagDsl
interface ConfigScope {
    /**
     * Define a flag using infix syntax.
     *
     * Example:
     * ```kotlin
     * config {
     *     MyFeature.FEATURE_A with {
     *         default(true)
     *     }
     * }
     * ```
     *
     * @param S The EncodableValue type wrapping the actual value
     * @param T The actual value type
     * @param C The context type the flag evaluates against
     * @param build DSL block for configuring the flag
     */
    infix fun <S : EncodableValue<T>, T : Any, C : Context> Feature<S, T, C>.with(build: FlagScope<S, T, C>.() -> Unit)
}
