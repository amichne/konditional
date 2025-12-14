package io.amichne.konditional.fix

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.dimension.Dimensions
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.DimensionScope
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry.Companion.updateDefinition
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.builders.DimensionBuilder
import io.amichne.konditional.internal.builders.FlagBuilder

/**
 * Updates this feature using a DSL configuration block.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param function The DSL configuration block
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.update(
    default: T,
    function: FlagScope<S, T, C, M>.() -> Unit,
): Unit = namespace.updateDefinition(FlagBuilder(default, this).apply(function).build())

/**
 * Updates this feature's definition in the namespace.
 *
 * **Internal API**: This method is used internally and should not be called directly.
 * When using FeatureContainer, configuration is handled automatically through delegation.
 *
 * @param definition The flag definition to update
 */
internal fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> Feature<S, T, C, M>.update(
    definition: FlagDefinition<S, T, C, M>,
): Unit = namespace.updateDefinition(definition)

/**
 * Top-level DSL function to create [io.amichne.konditional.context.dimension.Dimensions]
 *
 * Internal API: This function is used internally for testing and should not be called directly.
 *
 * Example:
 *   val dims = dimensions {
 *       environment(Environment.PROD)
 *       tenant(Tenant.SME)
 *   }
 */

internal fun dimensions(block: DimensionScope.() -> Unit): Dimensions =
    DimensionBuilder().apply(block).build()
