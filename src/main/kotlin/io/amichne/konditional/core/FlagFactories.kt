package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.internal.builders.FlagBuilder

/**
 * Creates a FeatureFlag for this Conditional using a DSL builder.
 *
 * This is a convenience function for creating flags programmatically,
 * particularly useful in tests or when dynamic flag creation is needed.
 *
 * Example:
 * ```kotlin
 * val myFlag = MyFlags.FEATURE_A.flag {
 *     default(value = true)
 *     salt("v2")
 *     rule {
 *         platforms(Platform.IOS)
 *     } implies false
 * }
 * ```
 *
 * @param flagBuilder The DSL block for configuring the flag
 * @return A configured FeatureFlag instance
 */
@FeatureFlagDsl
fun <S : Any, C : Context> Conditional<S, C>.flag(
    flagBuilder: FlagBuilder<S, C>.() -> Unit = {},
): FeatureFlag<S, C> = FlagBuilder.run {
    this@flag.flag(flagBuilder)
}
