package io.amichne.konditional.core.registry

/**
 * Default runtime registry factory (ServiceLoader-discovered by `:konditional-core`).
 */
class InMemoryNamespaceRegistryFactory : NamespaceRegistryFactory {
    override fun create(namespaceId: String): NamespaceRegistry =
        InMemoryNamespaceRegistry(namespaceId = namespaceId)
}
