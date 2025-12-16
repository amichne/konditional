package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry.Companion.updateDefinition
import io.amichne.konditional.internal.builders.FlagBuilder

/**
 * Updates this feature using a DSL configuration block.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param function The DSL configuration block
 */
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.update(
    default: T,
    function: FlagScope<T, C>.() -> Unit,
): Unit = namespace.updateDefinition(FlagBuilder(default, this).apply(function).build())

/**
 * Updates this feature's definition in the namespace.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param definition The flag definition to override
 */
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.update(
    definition: FlagDefinition<T, C, M>,
): Unit = namespace.updateDefinition(definition)
