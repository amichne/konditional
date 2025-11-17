package io.amichne.konditional.core.features

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.registry.ModuleRegistry
import io.amichne.konditional.core.registry.ModuleRegistry.Companion.updateDefinition
import io.amichne.konditional.core.registry.RegistryScope
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.FlagBuilder

fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> Feature<S, T, C, M>.evaluate(
    context: C,
    registry: ModuleRegistry = RegistryScope.current()
): T? = registry.featureFlag(this)?.evaluate(context)

/**
 * Updates this feature using a DSL configuration block.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param function The DSL configuration block
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> Feature<S, T, C, M>.update(
    function: FlagScope<S, T, C, M>.() -> Unit
): Unit = module.updateDefinition(FlagBuilder(this).apply(function).build())

/**
 * Updates this feature's definition in the taxonomy.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param definition The flag definition to update
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> Feature<S, T, C, M>.update(
    definition: FlagDefinition<S, T, C, M>
): Unit = module.updateDefinition(definition)
