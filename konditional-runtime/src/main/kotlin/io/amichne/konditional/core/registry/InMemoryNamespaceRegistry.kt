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
 * ## Atomicity
 *
 * Reads and writes are linearizable at the [NamespaceSnapshot] boundary.
 * A single `AtomicReference<NamespaceSnapshot>` holds the current snapshot per namespace.
 * All writes (load, rollback, updateDefinition) go through a `writeLock` to keep the
 * `current` and `historyRef` consistent with each other. Reads never acquire a lock.
 *
 * Readers will observe either the complete previous snapshot or the complete new snapshot —
 * never a partially-applied configuration.
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
    private val current = AtomicReference(NamespaceSnapshot.empty)
    private val hooksRef = AtomicReference(hooks)
    private val allDisabled = AtomicBoolean(false)
    private val historyRef = AtomicReference<List<NamespaceSnapshot>>(emptyList())
    private val writeLock = Any()

    private val overrides = ConcurrentHashMap<Feature<*, *, *>, AtomicReference<List<Any>>>()

    override fun load(config: ConfigurationView) {
        val concrete = config.toConcrete()
        val newSnapshot = NamespaceSnapshot(concrete)

        synchronized(writeLock) {
            val previous = current.getAndSet(newSnapshot)
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
        get() = current.get().configuration

    /**
     * The current [NamespaceSnapshot] held by this registry.
     *
     * Callers observing this value are guaranteed to see either a complete previous snapshot
     * or a complete new snapshot — never partial state.
     */
    val currentSnapshot: NamespaceSnapshot
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
        get() = historyRef.get().map { it.configuration }

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
                version = restored.version,
            ),
        )

        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any, C : Context, M : Namespace> flag(key: Feature<T, C, M>): FlagDefinition<T, C, M> {
        return findFlag(key)
            ?: throw IllegalStateException("Flag not found in configuration: ${key.key}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any, C : Context, M : Namespace> findFlag(key: Feature<T, C, M>): FlagDefinition<T, C, M>? {
        val override = overrides.getOverride(key)
        return if (override != null) {
            flagDefinitionFromSerialized(
                feature = key,
                defaultValue = override,
                rules = emptyList(),
                metadata = SerializedFlagDefinitionMetadata(isActive = true),
            )
        } else {
            configuration.flags[key] as? FlagDefinition<T, C, M>
        }
    }

    override fun updateDefinition(definition: FlagDefinition<*, *, *>) {
        current.updateAndGet { snapshot ->
            val mutableFlags = snapshot.configuration.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            NamespaceSnapshot(Configuration(mutableFlags, snapshot.configuration.metadata))
        }
    }

    override fun <T : Any, C : Context, M : Namespace> setOverride(
        feature: Feature<T, C, M>,
        value: T,
    ) {
        overrides.computeIfAbsent(feature) { AtomicReference(emptyList()) }
            .updateAndGet { stack -> stack + (value as Any) }
    }

    override fun <T : Any, C : Context, M : Namespace> clearOverride(
        feature: Feature<T, C, M>,
    ) {
        val stackRef = overrides[feature] ?: return
        val updated = stackRef.updateAndGet { stack ->
            if (stack.isEmpty()) {
                stack
            } else {
                stack.dropLast(1)
            }
        }
        if (updated.isEmpty()) {
            overrides.remove(feature, stackRef)
        }
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT: Int = 10
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any, C : Context, M : Namespace>
ConcurrentHashMap<Feature<*, *, *>, AtomicReference<List<Any>>>.getOverride(
    feature: Feature<T, C, M>,
): T? = this[feature]?.get()?.lastOrNull() as? T

private fun ConfigurationView.toConcrete(): Configuration =
    (this as? Configuration)
        ?: Configuration(
            flags = flags.toMap(),
            metadata = metadata.toConcrete(),
        )

private fun ConfigurationMetadataView.toConcrete(): ConfigurationMetadata =
    (this as? ConfigurationMetadata)
        ?: ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
