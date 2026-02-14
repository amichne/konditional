@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class NamespaceLinearizabilityTest {

    @Test
    fun `load rollback history and evaluation remain coherent under contention`() {
        val historyLimit = InMemoryNamespaceRegistry.DEFAULT_HISTORY_LIMIT
        val namespace = linearizableNamespace(historyLimit = historyLimit)
        val snapshots = (1..4).associateWith { value -> versionedConfiguration(namespace, value) }
        namespace.load(snapshots.getValue(1))

        val start = CountDownLatch(1)
        val done = CountDownLatch(8)
        val failure = AtomicReference<Throwable?>(null)
        val allowedValues = snapshots.keys
        val executor = Executors.newFixedThreadPool(8)

        submitWorker(executor, start, done, failure) {
            repeat(400) {
                if (failure.get() != null) return@repeat

                namespace.load(snapshots.getValue(2))
                assertTrue(namespace.rollback(1), "rollback after load(2) must succeed")
                namespace.load(snapshots.getValue(3))
                assertTrue(namespace.rollback(1), "rollback after load(3) must succeed")
                namespace.load(snapshots.getValue(4))
                namespace.load(snapshots.getValue(1))
            }
        }

        repeat(4) {
            submitWorker(executor, start, done, failure) {
                repeat(25_000) {
                    val observed = namespace.primary.evaluate(testContext)
                    if (observed !in allowedValues) {
                        error("Observed value outside loaded snapshots: $observed")
                    }
                }
            }
        }

        repeat(2) {
            submitWorker(executor, start, done, failure) {
                repeat(10_000) {
                    assertSnapshotCoherent(namespace.configuration, namespace)
                }
            }
        }

        submitWorker(executor, start, done, failure) {
            repeat(10_000) {
                assertHistoryCoherent(namespace.history, namespace, historyLimit)
            }
        }

        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS), "Workers did not finish in time")
        failure.get()?.let { throw AssertionError("Concurrent load/rollback/evaluation failed", it) }

        assertSnapshotCoherent(namespace.configuration, namespace)
        assertHistoryCoherent(namespace.history, namespace, historyLimit)
        executor.shutdownNow()
    }

    @Test
    fun `rollback progression stays linearizable while evaluations run`() {
        val historyLimit = 64
        val namespace = linearizableNamespace(historyLimit = historyLimit)
        val snapshots = (1..20).associateWith { value -> versionedConfiguration(namespace, value) }
        snapshots.values.forEach(namespace::load)

        val rollingBack = AtomicBoolean(true)
        val start = CountDownLatch(1)
        val done = CountDownLatch(6)
        val failure = AtomicReference<Throwable?>(null)
        val executor = Executors.newFixedThreadPool(6)
        val allowedValues = snapshots.keys

        submitWorker(executor, start, done, failure) {
            repeat(19) {
                assertTrue(namespace.rollback(1), "rollback should succeed while history has snapshots")
                assertSnapshotCoherent(namespace.configuration, namespace)
            }
            rollingBack.set(false)
        }

        repeat(4) {
            submitWorker(executor, start, done, failure) {
                while (rollingBack.get()) {
                    val observed = namespace.primary.evaluate(testContext)
                    if (observed !in allowedValues) {
                        error("Observed value outside loaded snapshots during rollback: $observed")
                    }
                }
            }
        }

        submitWorker(executor, start, done, failure) {
            while (rollingBack.get()) {
                assertHistoryCoherent(namespace.history, namespace, historyLimit)
            }
        }

        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS), "Workers did not finish in time")
        failure.get()?.let { throw AssertionError("Concurrent rollback/evaluation failed", it) }

        assertEquals(1, namespace.primary.evaluate(testContext), "final rollback target must be version 1")
        assertEquals(1, namespace.history.size, "history should retain only the initial registry snapshot")
        assertSnapshotCoherent(namespace.history.single(), namespace)
        assertEquals(0, namespace.history.single().intValue(namespace.primary))
        executor.shutdownNow()
    }

    private fun submitWorker(
        executor: java.util.concurrent.ExecutorService,
        start: CountDownLatch,
        done: CountDownLatch,
        failure: AtomicReference<Throwable?>,
        work: () -> Unit,
    ) {
        executor.submit {
            try {
                start.await()
                work()
            } catch (t: Throwable) {
                failure.compareAndSet(null, t)
            } finally {
                done.countDown()
            }
        }
    }

    private fun linearizableNamespace(
        historyLimit: Int,
    ): LinearizableNamespace {
        val id = "linearizable-${UUID.randomUUID()}"
        return LinearizableNamespace(
            id = id,
            historyLimit = historyLimit,
        )
    }

    private fun versionedConfiguration(
        namespace: LinearizableNamespace,
        value: Int,
    ): MaterializedConfiguration =
        MaterializedConfiguration.of(
            schema = namespace.compiledSchema(),
            configuration = Configuration(
                flags = mapOf(
                    namespace.primary to intDefinition(namespace.primary, value),
                    namespace.mirror to intDefinition(namespace.mirror, -value),
                ),
                metadata = ConfigurationMetadata(version = "v$value"),
            ),
        )

    private fun assertHistoryCoherent(
        history: List<ConfigurationView>,
        namespace: LinearizableNamespace,
        historyLimit: Int,
    ) {
        assertTrue(history.size <= historyLimit, "history must respect configured limit")
        history.forEach { snapshot ->
            assertSnapshotCoherent(snapshot, namespace)
        }
    }

    private fun assertSnapshotCoherent(
        snapshot: ConfigurationView,
        namespace: LinearizableNamespace,
    ) {
        if (snapshot.flags.isEmpty()) {
            assertNull(snapshot.metadata.version, "empty initial snapshot should not have metadata version")
            return
        }

        val primaryValue = snapshot.intValue(namespace.primary)
        val mirrorValue = snapshot.intValue(namespace.mirror)

        assertEquals(-primaryValue, mirrorValue, "mirror must remain the negated primary value")
        if (snapshot.metadata.version == null) {
            assertEquals(0, primaryValue, "unversioned snapshot should only be the declared default snapshot")
            return
        }

        assertEquals("v$primaryValue", snapshot.metadata.version, "metadata version must match primary value")
    }

    private fun <M : Namespace> intDefinition(
        feature: Feature<Int, Context, M>,
        value: Int,
    ): FlagDefinition<Int, Context, M> =
        FlagDefinition(
            feature = feature,
            bounds = emptyList(),
            defaultValue = value,
        )

    @Suppress("UNCHECKED_CAST")
    private fun <M : Namespace> ConfigurationView.intValue(
        feature: Feature<Int, Context, M>,
    ): Int =
        (flags[feature] as? FlagDefinition<Int, Context, M>)
            ?.defaultValue
            ?: error("Missing feature '${feature.key}' in snapshot")

    private class LinearizableNamespace(
        id: String,
        historyLimit: Int,
    ) : Namespace(
            id = id,
            registry = InMemoryNamespaceRegistry(namespaceId = id, historyLimit = historyLimit),
            identifierSeed = "${id}-seed",
        ) {
            val primary by integer<Context>(default = 0)
            val mirror by integer<Context>(default = 0)
        }

    companion object {
        private val testContext =
            Context(
                locale = AppLocale.UNITED_STATES,
                platform = Platform.IOS,
                appVersion = Version.parse("1.0.0").getOrThrow(),
                stableId = StableId.of("linearizability-test-user"),
            )
    }
}
