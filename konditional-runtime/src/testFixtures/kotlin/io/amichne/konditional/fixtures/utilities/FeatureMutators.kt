@file:OptIn(io.amichne.konditional.internal.KonditionalInternalApi::class)

package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime
import io.amichne.konditional.internal.builders.FlagBuilder

/**
 * Updates this feature using a DSL configuration block.
 *
 * **Test Fixture API**: This method is provided for test code and should not be used in production.
 * When defining flags on a namespace, configuration is handled automatically through delegation.
 *
 * @param function The DSL configuration block
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.update(
    default: T,
    function: FlagScope<T, C>.() -> Unit,
): Unit =
    (namespace.registry as? NamespaceRegistryRuntime)
        ?.updateDefinition(FlagBuilder(default, this).apply(function).build())
        ?: error("NamespaceRegistryRuntime is required. Add :konditional-runtime to your dependencies.")

/**
 * Updates this feature's definition in the namespace.
 *
 * **Test Fixture API**: This method is provided for test code and should not be used in production.
 * When defining flags on a namespace, configuration is handled automatically through delegation.
 *
 * @param definition The flag definition to override
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.update(
    definition: FlagDefinition<T, C, M>,
): Unit =
    (namespace.registry as? NamespaceRegistryRuntime)
        ?.updateDefinition(definition)
        ?: error("NamespaceRegistryRuntime is required. Add :konditional-runtime to your dependencies.")
