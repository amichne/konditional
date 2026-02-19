file=konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt
package=io.amichne.konditional.core.registry
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.instance.ConfigurationView,io.amichne.konditional.core.ops.RegistryHooks
type=io.amichne.konditional.core.registry.NamespaceRegistry|kind=interface|decl=interface NamespaceRegistry
type=io.amichne.konditional.core.registry.NamespaceRegistryRuntime|kind=interface|decl=interface NamespaceRegistryRuntime : NamespaceRegistry
fields:
- val namespaceId: String
- val configuration: ConfigurationView
- val hooks: RegistryHooks
methods:
- fun setHooks(hooks: RegistryHooks) val isAllDisabled: Boolean fun disableAll() fun enableAll() @Suppress("UNCHECKED_CAST") fun <T : Any, C : Context, M : Namespace> flag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>
- fun <T : Any, C : Context, M : Namespace> findFlag( key: Feature<T, C, M>, ): FlagDefinition<T, C, M>?
- fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
- fun load(config: ConfigurationView) val history: List<ConfigurationView> fun rollback(steps: Int = 1): Boolean
