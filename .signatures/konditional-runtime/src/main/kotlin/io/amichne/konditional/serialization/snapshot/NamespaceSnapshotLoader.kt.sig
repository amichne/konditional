file=konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt
package=io.amichne.konditional.serialization.snapshot
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.registry.NamespaceRegistryRuntime,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseErrorOrNull,io.amichne.konditional.core.result.parseFailure,io.amichne.konditional.serialization.instance.MaterializedConfiguration,io.amichne.konditional.serialization.options.SnapshotLoadOptions
type=io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader|kind=class|decl=class NamespaceSnapshotLoader<M : Namespace>( private val namespace: M, private val codec: SnapshotCodec<MaterializedConfiguration> = ConfigurationSnapshotCodec, ) : SnapshotLoader<MaterializedConfiguration>
methods:
- override fun load( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>
- private fun decodeSnapshot( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>
- private fun Namespace.runtimeRegistry(): NamespaceRegistryRuntime
- private fun ParseError.withNamespaceContext(namespaceId: String): ParseError
