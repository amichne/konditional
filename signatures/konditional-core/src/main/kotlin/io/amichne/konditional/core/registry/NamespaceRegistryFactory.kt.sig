file=konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistryFactory.kt
package=io.amichne.konditional.core.registry
imports=java.util.ServiceLoader
type=io.amichne.konditional.core.registry.NamespaceRegistryFactory|kind=interface|decl=fun interface NamespaceRegistryFactory
type=io.amichne.konditional.core.registry.NamespaceRegistryFactories|kind=object|decl=internal object NamespaceRegistryFactories
fields:
- private val factories: List<NamespaceRegistryFactory> by lazy(LazyThreadSafetyMode.PUBLICATION)
methods:
- fun default(namespaceId: String): NamespaceRegistry
