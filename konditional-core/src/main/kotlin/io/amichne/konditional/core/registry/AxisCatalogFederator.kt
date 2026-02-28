package io.amichne.konditional.core.registry

import io.amichne.konditional.context.axis.Axis

/**
 * Central axis catalog federator for sharing axis definitions across namespaces.
 *
 * The federator owns a global catalog where a platform can register the canonical set of axes
 * it supports. Each integrating namespace can then request a child catalog via
 * [namespaceCatalog], which preserves namespace-level isolation while allowing type-inferred DSL
 * lookups to seamlessly resolve axes provided by the federator.
 *
 * This design keeps namespace mutation local and deterministic: readers observe either local axes,
 * inherited federated axes, or both, with no cross-namespace writes.
 */
class AxisCatalogFederator(
    private val globalCatalog: AxisCatalog = AxisCatalog(),
) {
    /**
     * Registers [axis] in the global federated catalog.
     */
    fun register(axis: Axis<*>) {
        globalCatalog.register(axis)
    }

    /**
     * Returns a namespace-scoped catalog inheriting from the global federated catalog.
     */
    fun namespaceCatalog(): AxisCatalog = AxisCatalog(globalCatalog)
}
