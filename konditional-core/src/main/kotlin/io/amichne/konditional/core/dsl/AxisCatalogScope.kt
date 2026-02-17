package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.registry.AxisCatalog

/**
 * Internal bridge for DSL scopes that can resolve inferred axis values through a scoped catalog.
 */
internal interface AxisCatalogScope {
    val axisCatalog: AxisCatalog?
}
