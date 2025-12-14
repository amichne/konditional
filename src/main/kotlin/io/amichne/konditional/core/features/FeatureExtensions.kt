package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.types.EncodableValue

/**
 * Evaluates this feature for the given contextFn.
 *
 * By default, evaluates using the feature's namespace registry.
 * For testing, you can provide an explicit registry parameter.
 *
 * @param context The evaluation contextFn
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return The evaluated value, or null if the feature is not registered
 */
fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T = registry.flag(this).evaluate(context)
