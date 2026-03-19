@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.core.registry

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.fixtures.TestContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegistryHistoryTest {

    private fun makeRegistry(namespaceId: String = "hist-test") = InMemoryNamespaceRegistry(namespaceId)

    private fun flagConfig(ns: HistoryNs, value: Int, version: String) = Configuration(
        flags = mapOf(ns.counter to FlagDefinition(feature = ns.counter, bounds = emptyList(), defaultValue = value)),
        metadata = ConfigurationMetadata(version = version),
    )

    private class HistoryNs(
        registry: InMemoryNamespaceRegistry,
    ) : Namespace.TestNamespaceFacade(
            id = "hist",
            registry = registry,
            identifierSeed = "hist-seed",
        ) {
            val counter by integer<Context>(default = 0)
        }

    @Test
    fun `history grows with each load`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)

        reg.load(flagConfig(ns, 1, "v1"))
        reg.load(flagConfig(ns, 2, "v2"))
        reg.load(flagConfig(ns, 3, "v3"))

        // History is: [empty_snapshot(null), v1_snapshot, v2_snapshot]; current is v3
        assertEquals(3, reg.history.size)
        assertNull(reg.history[0].version) // initial empty snapshot
        assertEquals("v1", reg.history[1].version)
        assertEquals("v2", reg.history[2].version)
        assertEquals("v3", reg.currentSnapshot.version)
    }

    @Test
    fun `rollback restores prior snapshot`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)

        reg.load(flagConfig(ns, 1, "v1"))
        reg.load(flagConfig(ns, 2, "v2"))

        assertTrue(reg.rollback())
        assertEquals(1, ns.counter.evaluate(TestContext()))
        assertEquals("v1", reg.currentSnapshot.version)
    }

    @Test
    fun `multi-step rollback restores correct prior snapshot`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)

        reg.load(flagConfig(ns, 1, "v1"))
        reg.load(flagConfig(ns, 2, "v2"))
        reg.load(flagConfig(ns, 3, "v3"))

        assertTrue(reg.rollback(steps = 2))
        assertEquals(1, ns.counter.evaluate(TestContext()))
        assertEquals("v1", reg.currentSnapshot.version)
    }

    @Test
    fun `rollback returns false when history is empty`() {
        val reg = makeRegistry()
        assertFalse(reg.rollback())
    }

    @Test
    fun `rollback returns false when steps exceeds history size`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)

        reg.load(flagConfig(ns, 1, "v1"))
        // Only 0 snapshots in history (current is v1, history is empty before any load)
        assertFalse(reg.rollback(steps = 2))
    }

    @Test
    fun `history limit is respected`() {
        val reg = InMemoryNamespaceRegistry(namespaceId = "hist-limit", historyLimit = 3)
        val ns = HistoryNs(reg)

        repeat(10) { i ->
            reg.load(flagConfig(ns, i, "v$i"))
        }

        assertEquals(3, reg.history.size)
    }

    @Test
    fun `disableAll is idempotent`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)
        reg.load(flagConfig(ns, 1, "v1"))
        // History after load: [empty_snapshot, v1_enabled] — no, wait:
        // load → previous=empty, next=v1_snapshot, different → history=[empty], current=v1
        // First disableAll → previous=v1_enabled, next=v1_disabled, different → history=[empty, v1_enabled], current=v1_disabled

        reg.disableAll()
        val snapshotAfterFirst = reg.currentSnapshot
        val historySizeAfterFirst = reg.history.size

        // Second disableAll should be a no-op (snapshot already disabled)
        reg.disableAll()
        val snapshotAfterSecond = reg.currentSnapshot

        assertEquals(snapshotAfterFirst, snapshotAfterSecond)
        assertEquals(historySizeAfterFirst, reg.history.size)
        assertTrue(reg.isAllDisabled)
    }

    @Test
    fun `enableAll is idempotent when already enabled`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)
        reg.load(flagConfig(ns, 1, "v1"))

        val historySizeBefore = reg.history.size

        // enableAll when not disabled should be a no-op
        reg.enableAll()

        assertEquals(historySizeBefore, reg.history.size)
    }

    @Test
    fun `disableAll then enableAll round trips correctly`() {
        val reg = makeRegistry()
        val ns = HistoryNs(reg)
        reg.load(flagConfig(ns, 1, "v1"))

        reg.disableAll()
        assertTrue(reg.isAllDisabled)

        reg.enableAll()
        assertFalse(reg.isAllDisabled)
        assertEquals(1, ns.counter.evaluate(TestContext()))
    }

    @Test
    fun `history is empty before any load`() {
        val reg = makeRegistry()
        assertTrue(reg.history.isEmpty())
        assertEquals(NamespaceSnapshot.empty, reg.currentSnapshot)
    }
}
