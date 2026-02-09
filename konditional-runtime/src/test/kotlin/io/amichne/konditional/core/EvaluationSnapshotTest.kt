@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.explain
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.runtime.load
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EvaluationSnapshotTest {
    private data class SnapshotContext(val x: Int) : Context

    @Test
    fun `snapshot evaluation pins configuration version`() {
        val registry = InMemoryNamespaceRegistry("snapshot-eval")
        val namespace = object : Namespace("snapshot-eval", registry) {
            val flag by boolean<SnapshotContext>(default = false) {
                rule(true) {
                    extension { x > 0 }
                }
            }
        }

        val baseline = Configuration(
            flags = namespace.configuration.flags,
            metadata = ConfigurationMetadata(version = "v1"),
        )
        namespace.load(baseline)
        val snapshot = namespace.snapshot()

        namespace.load(baseline.withMetadata(version = "v2"))

        val result = namespace.flag.explain(SnapshotContext(1), snapshot)
        assertEquals("v1", result.configVersion)
    }

    @Test
    fun `snapshot capture is safe under concurrent override mutation`() {
        val registry = InMemoryNamespaceRegistry("snapshot-concurrency")
        val namespace = object : Namespace("snapshot-concurrency", registry) {
            val overridable by boolean<SnapshotContext>(default = false)
            val stable by boolean<SnapshotContext>(default = true)
        }

        val workers = 6
        val iterationsPerWorker = 2_000
        val executor = Executors.newFixedThreadPool(workers)
        val completionLatch = CountDownLatch(workers)
        val failures = ConcurrentLinkedQueue<Throwable>()

        repeat(workers) { worker ->
            executor.submit {
                try {
                    repeat(iterationsPerWorker) { iteration ->
                        val context = SnapshotContext(worker * iterationsPerWorker + iteration)
                        if (iteration % 3 == 0) {
                            registry.setOverride(namespace.overridable, iteration % 2 == 0)
                            if (iteration % 5 == 0) {
                                registry.clearOverride(namespace.overridable)
                            }
                        } else {
                            val snapshot = namespace.snapshot()
                            namespace.overridable.evaluate(context, snapshot)
                            namespace.stable.evaluate(context, snapshot)
                        }
                    }
                } catch (throwable: Throwable) {
                    failures.add(throwable)
                } finally {
                    completionLatch.countDown()
                }
            }
        }

        assertTrue(completionLatch.await(20, TimeUnit.SECONDS), "Concurrent snapshot workers timed out")
        executor.shutdownNow()
        assertTrue(failures.isEmpty(), "Concurrent snapshotting failed: $failures")
    }
}
