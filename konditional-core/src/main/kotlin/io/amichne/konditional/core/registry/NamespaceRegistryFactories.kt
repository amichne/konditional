package io.amichne.konditional.core.registry

import java.util.ServiceLoader

internal object NamespaceRegistryFactories {
    private val factories: List<NamespaceRegistryFactory> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        runCatching {
            ServiceLoader.load(NamespaceRegistryFactory::class.java).toList()
        }.getOrElse { emptyList() }
    }

    fun default(namespaceId: String): NamespaceRegistry =
        when (factories.size) {
            0 ->
                error(
                    "No NamespaceRegistryFactory found. " +
                        "Add :konditional-runtime to your dependencies to enable runtime registry operations " +
                        "(Gradle: implementation(\"io.amichne:konditional-runtime:<version>\")).",
                )

            1 -> factories.single().create(namespaceId)
            else ->
                error(
                    "Multiple NamespaceRegistryFactory implementations found (${factories.size}). " +
                        "Ensure only one runtime module is on the classpath.",
                )
        }
}
