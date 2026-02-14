file=konditional-runtime/src/test/kotlin/io/amichne/konditional/runtime/NamespaceLinearizabilityTest.kt
package=io.amichne.konditional.runtime
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.instance.ConfigurationView,io.amichne.konditional.core.registry.InMemoryNamespaceRegistry,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.ConfigurationMetadata,java.util.UUID,java.util.concurrent.CountDownLatch,java.util.concurrent.Executors,java.util.concurrent.TimeUnit,java.util.concurrent.atomic.AtomicBoolean,java.util.concurrent.atomic.AtomicReference,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertNull,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.runtime.NamespaceLinearizabilityTest|kind=class|decl=class NamespaceLinearizabilityTest
type=io.amichne.konditional.runtime.LinearizableNamespace|kind=class|decl=private class LinearizableNamespace( id: String, historyLimit: Int, ) : Namespace( id = id, registry = InMemoryNamespaceRegistry(namespaceId = id, historyLimit = historyLimit), identifierSeed = "${id}-seed", )
fields:
- val primary by integer<Context>(default = 0)
- val mirror by integer<Context>(default = 0)
methods:
- fun `load rollback history and evaluation remain coherent under contention`()
- fun `rollback progression stays linearizable while evaluations run`()
- private fun submitWorker( executor: java.util.concurrent.ExecutorService, start: CountDownLatch, done: CountDownLatch, failure: AtomicReference<Throwable?>, work: () -> Unit, )
- private fun linearizableNamespace( historyLimit: Int, ): LinearizableNamespace
- private fun versionedConfiguration( namespace: LinearizableNamespace, value: Int, ): Configuration
- private fun assertHistoryCoherent( history: List<ConfigurationView>, namespace: LinearizableNamespace, historyLimit: Int, )
- private fun assertSnapshotCoherent( snapshot: ConfigurationView, namespace: LinearizableNamespace, )
- private fun <M : Namespace> intDefinition( feature: Feature<Int, Context, M>, value: Int, ): FlagDefinition<Int, Context, M>
- private fun <M : Namespace> ConfigurationView.intValue( feature: Feature<Int, Context, M>, ): Int
