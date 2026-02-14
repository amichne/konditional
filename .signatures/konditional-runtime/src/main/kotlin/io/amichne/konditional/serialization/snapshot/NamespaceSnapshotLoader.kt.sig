file=konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt
package=io.amichne.konditional.serialization.snapshot
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.registry.NamespaceRegistryRuntime,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.options.SnapshotLoadOptions
type=io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader|kind=class|decl=class NamespaceSnapshotLoader<M : Namespace>( private val namespace: M, private val codec: SnapshotCodec<Configuration> = ConfigurationSnapshotCodec, ) : SnapshotLoader<Configuration>
methods:
- override fun load( json: String, options: SnapshotLoadOptions, ): ParseResult<Configuration>
- private fun decodeSnapshot( json: String, options: SnapshotLoadOptions, ): ParseResult<Configuration>
- private fun Namespace.runtimeRegistry(): NamespaceRegistryRuntime
- private fun ParseError.withNamespaceContext(namespaceId: String): ParseError
