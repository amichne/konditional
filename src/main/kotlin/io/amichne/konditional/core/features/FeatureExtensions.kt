package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistry.Companion.updateDefinition
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.FlagBuilder

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
    registry: NamespaceRegistry = namespace
): T = registry.flag(this).evaluate(context)

/**
 * Updates this feature using a DSL configuration block.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param function The DSL configuration block
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.update(
    function: FlagScope<S, T, C, M>.() -> Unit
): Unit = namespace.updateDefinition(FlagBuilder(this).apply(function).build())

/**
 * Updates this feature's definition in the namespace.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param definition The flag definition to update
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.update(
    definition: FlagDefinition<S, T, C, M>
): Unit = namespace.updateDefinition(definition)
