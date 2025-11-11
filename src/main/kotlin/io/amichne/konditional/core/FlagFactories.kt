package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.internal.builders.FlagBuilder

/**
 * Creates a FlagDefinition for this Feature using a DSL builder.
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
 * @param flagBuilder The DSL block for configuring the flag. The receiver is [FlagScope],
 *                    a sealed interface that defines the public DSL API.
 * @return A configured FlagDefinition instance
 */
@FeatureFlagDsl
inline fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> Feature<S, T, C>.flag(
    flagBuilder: FlagScope<S, T, C>.() -> Unit = {},
): FlagDefinition<S, T, C> = FlagBuilder(this).apply(flagBuilder).build()
