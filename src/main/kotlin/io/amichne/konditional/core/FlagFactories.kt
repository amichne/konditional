package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.types.EncodableValue
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
 *     } returns false
 * }
 * ```
 *
 * @param flagBuilder The DSL block for configuring the flag. The receiver is [io.amichne.konditional.core.dsl.FlagScope],
 *                    a sealed interface that defines the public DSL API.
 * @return A configured FlagDefinition instance
 */
@KonditionalDsl
inline fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.flag(
    flagBuilder: FlagScope<S, T, C, M>.() -> Unit = {},
): FlagDefinition<S, T, C, M> = FlagBuilder(this).apply(flagBuilder).build()
