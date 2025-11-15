package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.types.EncodableValue
import org.jetbrains.annotations.TestOnly

object RegistryScope {
    private val threadLocal = ThreadLocal<ModuleRegistry?>()

    /** Default global registry - can be swapped for testing if needed */
    @Volatile
    var global: ModuleRegistry = InMemoryModuleRegistry()
        private set

    /** Get the current registry: thread-local override or global default */
    fun current(): ModuleRegistry = threadLocal.get() ?: global

    /** Run a block with a specific registry in scope */

    /** Replace global registry (testing only) */
    @TestOnly
    fun setGlobal(registry: ModuleRegistry) {
        global = registry
    }

    @TestOnly
    fun <T> testRegistryScope(block: () -> T): T = with(InMemoryModuleRegistry(), block)

    fun <T> with(registry: ModuleRegistry, block: () -> T): T {
        val previous = threadLocal.get()
        threadLocal.set(registry)
        try {
            return block()
        } finally {
            if (previous != null) threadLocal.set(previous)
            else threadLocal.remove()
        }
    }
}

fun <S : EncodableValue<T>, T : Any, C : Context, M : Taxonomy> Feature<S, T, C, M>.evaluate(
    context: C,
    registry: ModuleRegistry = RegistryScope.current()
): T? = registry.featureFlag(this)?.evaluate(context)
