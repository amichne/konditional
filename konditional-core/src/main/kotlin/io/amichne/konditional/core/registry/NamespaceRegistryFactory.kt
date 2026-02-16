package io.amichne.konditional.core.registry

import java.util.ServiceLoader

/**
 * SPI for providing a default [NamespaceRegistry] implementation.
 *
 * Implemented by `:konditional-runtime` and discovered via [ServiceLoader] so that `:konditional-core`
 * does not depend on runtime implementations.
 */
fun interface NamespaceRegistryFactory {
    fun create(namespaceId: String): NamespaceRegistry
}
