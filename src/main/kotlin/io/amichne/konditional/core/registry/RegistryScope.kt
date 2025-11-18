package io.amichne.konditional.core.registry

import org.jetbrains.annotations.TestOnly

/**
 * Internal registry scope management for thread-local registry overrides.
 *
 * This object is internal to the library and should not be used directly by external consumers.
 * Registry access is handled automatically through Taxonomy.registry or can be scoped using
 * the public [withRegistry] function.
 */
internal object RegistryScope {
    private val threadLocal = ThreadLocal<ModuleRegistry?>()

    /** Default global registry - can be swapped for testing if needed */
    @Volatile
    var global: ModuleRegistry = InMemoryModuleRegistry()
        private set

    /** Get the current registry: thread-local override or global default */
    fun current(): ModuleRegistry = threadLocal.get() ?: global

    /** Replace global registry (testing only) */
    @TestOnly
    fun setGlobal(registry: ModuleRegistry) {
        global = registry
    }

    /** Run a block with a specific registry in scope */
    internal fun usingRegistry(registry: ModuleRegistry = current(), block: ModuleRegistry.() -> Unit): Unit {
        val previous = threadLocal.get()
        threadLocal.set(registry)
        try {
            return block(current())
        } finally {
            if (previous != null) threadLocal.set(previous)
            else threadLocal.remove()
        }
    }
}
