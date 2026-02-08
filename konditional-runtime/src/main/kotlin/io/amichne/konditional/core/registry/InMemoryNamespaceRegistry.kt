@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.registry

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.ConfigurationMetadataView
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.ops.RegistryHooks
import io.amichne.konditional.internal.SerializedFlagDefinitionMetadata
import io.amichne.konditional.internal.flagDefinitionFromSerialized
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory [NamespaceRegistryRuntime] implementation.
 *
 * Intended for:
 * - default runtime registry for [Namespace]
 * - tests requiring isolated registries
 */
class InMemoryNamespaceRegistry(
    override val namespaceId: String,
    hooks: RegistryHooks = RegistryHooks.None,
    private val historyLimit: Int = DEFAULT_HISTORY_LIMIT,
) : NamespaceRegistryRuntime {
    private val current = AtomicReference(Configuration(emptyMap()))
    private val hooksRef = AtomicReference(hooks)
    private val allDisabled = AtomicBoolean(false)
    private val historyRef = AtomicReference<List<Configuration>>(emptyList())
    private val writeLock = Any()

    private val overrides = ConcurrentHashMap<Feature<*, *, *>, ArrayDeque<Any>>()

    override fun load(config: ConfigurationView) {
        val concrete = config.toConcrete()

        synchronized(writeLock) {
            val previous = current.getAndSet(concrete)
            historyRef.set((historyRef.get() + previous).takeLast(historyLimit))
        }

        hooksRef.get().metrics.recordConfigLoad(
            Metrics.ConfigLoadMetric.of(
                namespaceId = namespaceId,
                featureCount = concrete.flags.size,
                version = concrete.metadata.version,
            ),
        )
    }

    override val configuration: ConfigurationView
        get() = current.get()

    override val hooks: RegistryHooks
        get() = hooksRef.get()

    override fun setHooks(hooks: RegistryHooks) {
        hooksRef.set(hooks)
    }

    override val isAllDisabled: Boolean
        get() = allDisabled.get()

    override fun disableAll() {
        allDisabled.set(true)
    }

    override fun enableAll() {
        allDisabled.set(false)
    }

    override val history: List<ConfigurationView>
        get() = historyRef.get()

    override fun rollback(steps: Int): Boolean {
        require(steps >= 1) { "steps must be >= 1" }

        val restored =
            synchronized(writeLock) {
                val history = historyRef.get()
                if (history.size < steps) return false

                val targetIndex = history.size - steps
                val target = history[targetIndex]
                val newHistory = history.take(targetIndex)

                current.set(target)
                historyRef.set(newHistory)
                target
            }

        hooksRef.get().metrics.recordConfigRollback(
            Metrics.ConfigRollbackMetric.of(
                namespaceId = namespaceId,
                steps = steps,
                success = true,
                version = restored.metadata.version,
            ),
        )

        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any, C : Context, M : Namespace> flag(key: Feature<T, C, M>): FlagDefinition<T, C, M> {
        return flagSnapshot(key, configuration)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any, C : Context, M : Namespace> flagSnapshot(
        key: Feature<T, C, M>,
        snapshot: ConfigurationView,
    ): FlagDefinition<T, C, M> =
        overrides.getOverride(key)
            ?.let { override ->
                flagDefinitionFromSerialized(
                    feature = key,
                    defaultValue = override,
                    rules = emptyList(),
                    metadata = SerializedFlagDefinitionMetadata(isActive = true),
                )
            }
            ?: snapshot.flags[key] as? FlagDefinition<T, C, M>
            ?: error("Flag not found in configuration: ${key.key}")

    override fun updateDefinition(definition: FlagDefinition<*, *, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Configuration(mutableFlags, currentSnapshot.metadata)
        }
    }

    override fun <T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<T, C, M>,
        value: T,
    ) {
        overrides.compute(feature) { _, stack ->
            val deque = stack ?: ArrayDeque()
            deque.addLast(value as Any)
            deque
        }
    }

    override fun <T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<T, C, M>,
    ) {
        overrides.compute(feature) { _, stack ->
            if (stack.isNullOrEmpty()) {
                null
            } else {
                stack.removeLast()
                if (stack.isEmpty()) null else stack
            }
        }
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT: Int = 10
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any, C : Context, M : Namespace> ConcurrentHashMap<Feature<*, *, *>, ArrayDeque<Any>>.getOverride(
    feature: Feature<T, C, M>,
): T? = this[feature]?.lastOrNull() as? T

private fun ConfigurationView.toConcrete(): Configuration =
    (this as? Configuration)
        ?: Configuration(
            flags = flags,
            metadata = metadata.toConcrete(),
        )

private fun ConfigurationMetadataView.toConcrete(): ConfigurationMetadata =
    (this as? ConfigurationMetadata)
        ?: ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
