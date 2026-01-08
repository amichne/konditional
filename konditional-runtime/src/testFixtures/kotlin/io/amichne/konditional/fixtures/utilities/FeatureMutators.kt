@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.fixtures.utilities

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistryRuntime

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
