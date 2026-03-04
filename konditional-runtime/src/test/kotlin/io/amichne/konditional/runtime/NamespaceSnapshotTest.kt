@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.runtime

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceSnapshot
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.ConfigurationMetadata
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NamespaceSnapshotTest {

    // ── NamespaceSnapshot structural tests ────────────────────────────────────

    @Test
    fun `NamespaceSnapshot empty has null version and empty flags`() {
        val empty = NamespaceSnapshot.empty
        assertNull(empty.version)
        assertTrue(empty.configuration.flags.isEmpty())
    }

    @Test
    fun `NamespaceSnapshot version delegates to configuration metadata`() {
        val config = Configuration(emptyMap(), ConfigurationMetadata(version = "v42"))
        val snapshot = NamespaceSnapshot(config)
        assertEquals("v42", snapshot.version)
    }

    @Test
    fun `NamespaceSnapshot is a data class with structural equality`() {
        val config = Configuration(emptyMap(), ConfigurationMetadata(version = "v1"))
        assertEquals(NamespaceSnapshot(config), NamespaceSnapshot(config))
    }

    // ── currentSnapshot on InMemoryNamespaceRegistry ──────────────────────────

    @Test
    fun `currentSnapshot starts as empty sentinel before any load`() {
        val reg = InMemoryNamespaceRegistry(namespaceId = "snap-ns")
        assertEquals(NamespaceSnapshot.empty, reg.currentSnapshot)
    }

    @Test
    fun `currentSnapshot reflects the most recently loaded configuration`() {
        val ns = object : Namespace.TestNamespaceFacade("snap-load-ns") {
            val flag by boolean<Context>(default = false)
        }
        val reg = InMemoryNamespaceRegistry(namespaceId = "snap-load-ns")

        val config = Configuration(
            flags = mapOf(ns.flag to FlagDefinition(feature = ns.flag, bounds = emptyList(), defaultValue = true)),
            metadata = ConfigurationMetadata(version = "v1"),
        )
        reg.load(config)

        assertEquals("v1", reg.currentSnapshot.version)
        assertNotNull(reg.currentSnapshot.configuration.flags[ns.flag])
    }

    // ── Atomicity smoke test: readers always see whole snapshots ──────────────

    /**
     * Verifies that readers observe only complete (old or new) snapshots — never
     * a version field from one snapshot paired with a flags map from another.
     *
     * The writer cycles between two versioned configurations (v1 and v2).
     * Each reader reads the currentSnapshot and checks that its version matches
     * the number of flags (v1 → 1 flag, v2 → 2 flags). A mismatch proves partial read.
     */
    @Test
    @Suppress("LongMethod")
    fun `readers always observe complete old-or-new snapshots under concurrent writes`() {
        val ns = object : Namespace.TestNamespaceFacade("snap-atomic-ns") {
            val flagA by boolean<Context>(default = false)
            val flagB by boolean<Context>(default = false)
        }
        val reg = InMemoryNamespaceRegistry(namespaceId = "snap-atomic-ns")

        val v1 = Configuration(
            flags = mapOf(ns.flagA to boolDef(ns.flagA, true)),
            metadata = ConfigurationMetadata(version = "v1"),
        )
        val v2 = Configuration(
            flags = mapOf(
                ns.flagA to boolDef(ns.flagA, true),
                ns.flagB to boolDef(ns.flagB, false),
            ),
            metadata = ConfigurationMetadata(version = "v2"),
        )
        reg.load(v1)

        val partialReadCount = AtomicInteger(0)
        val running = AtomicReference(true)
        val start = CountDownLatch(1)
        val done = CountDownLatch(5)
        val executor = Executors.newFixedThreadPool(5)

        // Writer: alternates between v1 and v2
        executor.submit {
            try {
                start.await()
                repeat(5_000) {
                    reg.load(if (it % 2 == 0) v1 else v2)
                }
            } finally {
                running.set(false)
                done.countDown()
            }
        }

        // Readers: verify snapshot coherence on each observation
        repeat(4) {
            executor.submit {
                try {
                    start.await()
                    while (running.get()) {
                        val snap = reg.currentSnapshot
                        val version = snap.version
                        val flagCount = snap.configuration.flags.size
                        // v1 has 1 flag, v2 has 2 flags — any other combination is a partial read
                        val coherent = when (version) {
                            "v1" -> flagCount == 1
                            "v2" -> flagCount == 2
                            null -> flagCount == 0 // initial empty
                            else -> true // unknown version is fine (shouldn't occur here)
                        }
                        if (!coherent) partialReadCount.incrementAndGet()
                    }
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS), "Workers did not finish in time")
        executor.shutdownNow()

        assertEquals(0, partialReadCount.get(), "Readers observed ${ partialReadCount.get()} partial snapshots")
    }

    // ── Namespace isolation ──────────────────────────────────────────────────

    @Test
    fun `loading into namespace A does not affect currentSnapshot of namespace B`() {
        val nsA = object : Namespace.TestNamespaceFacade("snap-iso-ns-a") {
            val flag by boolean<Context>(default = false)
        }
        val regA = InMemoryNamespaceRegistry(namespaceId = "snap-iso-ns-a")
        val regB = InMemoryNamespaceRegistry(namespaceId = "snap-iso-ns-b")

        regA.load(
            Configuration(
                flags = mapOf(nsA.flag to boolDef(nsA.flag, true)),
                metadata = ConfigurationMetadata(version = "a-v1"),
            ),
        )

        assertEquals("a-v1", regA.currentSnapshot.version)
        assertNull(regB.currentSnapshot.version, "regB snapshot version must remain null")
        assertTrue(regB.currentSnapshot.configuration.flags.isEmpty(), "regB flags must remain empty")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun <M : Namespace> boolDef(
        feature: io.amichne.konditional.core.features.Feature<Boolean, Context, M>,
        value: Boolean,
    ) = FlagDefinition(feature = feature, bounds = emptyList(), defaultValue = value)
}
