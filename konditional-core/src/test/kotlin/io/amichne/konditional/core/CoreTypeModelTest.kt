package io.amichne.konditional.core

import io.amichne.konditional.context.ContextKey
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisKey
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.external.ExternalSnapshotRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

private enum class TestTier : AxisValue<TestTier> {
    FREE, PRO;
    override val axis: Axis<TestTier> get() = Axis.of()
}

class CoreTypeModelTest {

    // ── ContextKey ─────────────────────────────────────────────────────────

    @Test
    fun `ContextKey equality is value-based`() {
        assertEquals(ContextKey("locale"), ContextKey("locale"))
        assertNotEquals(ContextKey("locale"), ContextKey("platform"))
    }

    @Test
    fun `ContextKey rejects blank id`() {
        assertFailsWith<IllegalArgumentException> { ContextKey("") }
        assertFailsWith<IllegalArgumentException> { ContextKey("   ") }
    }

    @Test
    fun `ContextKey toString contains the id`() {
        assertEquals("ContextKey(locale)", ContextKey("locale").toString())
    }

    // ── AxisKey ─────────────────────────────────────────────────────────────

    @Test
    fun `AxisKey equality is value-based`() {
        assertEquals(AxisKey("env"), AxisKey("env"))
        assertNotEquals(AxisKey("env"), AxisKey("tenant"))
    }

    @Test
    fun `AxisKey rejects blank id`() {
        assertFailsWith<IllegalArgumentException> { AxisKey("") }
        assertFailsWith<IllegalArgumentException> { AxisKey("  ") }
    }

    @Test
    fun `Axis dot key returns AxisKey wrapping Axis dot id`() {
        val axis = Axis.of<TestTier>()
        assertEquals(AxisKey(axis.id), axis.key)
    }

    @Test
    fun `AxisKey toString contains the id`() {
        assertEquals("AxisKey(env)", AxisKey("env").toString())
    }

    // ── ExternalSnapshotRef ─────────────────────────────────────────────────

    @Test
    fun `ExternalSnapshotRef Versioned accepts valid id and version`() {
        val ref = ExternalSnapshotRef.Versioned(id = "prices", version = "sha256:abc")
        assertEquals("prices", ref.id)
        assertEquals("sha256:abc", ref.version)
    }

    @Test
    fun `ExternalSnapshotRef Versioned rejects blank id`() {
        assertFailsWith<IllegalArgumentException> {
            ExternalSnapshotRef.Versioned(id = "", version = "v1")
        }
        assertFailsWith<IllegalArgumentException> {
            ExternalSnapshotRef.Versioned(id = "  ", version = "v1")
        }
    }

    @Test
    fun `ExternalSnapshotRef Versioned rejects blank version`() {
        assertFailsWith<IllegalArgumentException> {
            ExternalSnapshotRef.Versioned(id = "prices", version = "")
        }
        assertFailsWith<IllegalArgumentException> {
            ExternalSnapshotRef.Versioned(id = "prices", version = "   ")
        }
    }

    @Test
    fun `ExternalSnapshotRef factory method delegates to Versioned`() {
        val ref = ExternalSnapshotRef.versioned(id = "catalog", version = "42")
        assertEquals(ExternalSnapshotRef.Versioned(id = "catalog", version = "42"), ref)
    }

    @Test
    fun `ExternalSnapshotRef data class equality is structural`() {
        assertEquals(
            ExternalSnapshotRef.Versioned("a", "1"),
            ExternalSnapshotRef.Versioned("a", "1"),
        )
        assertNotEquals(
            ExternalSnapshotRef.Versioned("a", "1"),
            ExternalSnapshotRef.Versioned("a", "2"),
        )
    }
}
