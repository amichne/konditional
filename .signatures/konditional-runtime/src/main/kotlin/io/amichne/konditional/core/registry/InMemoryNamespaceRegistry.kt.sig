file=konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt
package=io.amichne.konditional.core.registry
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.instance.ConfigurationMetadataView,io.amichne.konditional.core.instance.ConfigurationView,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.core.ops.RegistryHooks,io.amichne.konditional.internal.SerializedFlagDefinitionMetadata,io.amichne.konditional.internal.flagDefinitionFromSerialized,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.ConfigurationMetadata,java.util.concurrent.ConcurrentHashMap,java.util.concurrent.atomic.AtomicBoolean,java.util.concurrent.atomic.AtomicReference
type=io.amichne.konditional.core.registry.InMemoryNamespaceRegistry|kind=class|decl=class InMemoryNamespaceRegistry( override val namespaceId: String, hooks: RegistryHooks = RegistryHooks.None, private val historyLimit: Int = DEFAULT_HISTORY_LIMIT, ) : NamespaceRegistryRuntime
fields:
- private val current
- private val hooksRef
- private val allDisabled
- private val historyRef
- private val writeLock
- private val overrides
- override val configuration: ConfigurationView
- override val hooks: RegistryHooks
- override val isAllDisabled: Boolean
- override val history: List<ConfigurationView>
methods:
- override fun load(config: ConfigurationView)
- override fun setHooks(hooks: RegistryHooks)
- override fun disableAll()
- override fun enableAll()
- override fun rollback(steps: Int): Boolean
- override fun <T : Any, C : Context, M : Namespace> flag(key: Feature<T, C, M>): FlagDefinition<T, C, M>
- override fun <T : Any, C : Context, M : Namespace> findFlag(key: Feature<T, C, M>): FlagDefinition<T, C, M>?
- override fun updateDefinition(definition: FlagDefinition<*, *, *>)
- override fun <T : Any, C : Context, M : Namespace> setOverride( feature: Feature<T, C, M>, value: T, )
- override fun <T : Any, C : Context, M : Namespace> clearOverride( feature: Feature<T, C, M>, )
