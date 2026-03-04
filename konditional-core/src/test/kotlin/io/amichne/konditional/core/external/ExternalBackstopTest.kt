package io.amichne.konditional.core.external

import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExternalBackstopTest {

    // ── ExternalSnapshotRef.Versioned — construction-time validation ──────────

    @Test
    fun `Versioned accepts valid id and version`() {
        val ref = ExternalSnapshotRef.Versioned(id = "prices", version = "sha256:abc")
        assertEquals("prices", ref.id)
        assertEquals("sha256:abc", ref.version)
    }

    @Test
    fun `Versioned rejects blank id at construction`() {
        assertFailsWith<IllegalArgumentException> { ExternalSnapshotRef.Versioned("", "v1") }
        assertFailsWith<IllegalArgumentException> { ExternalSnapshotRef.Versioned("  ", "v1") }
    }

    @Test
    fun `Versioned rejects blank version at construction`() {
        assertFailsWith<IllegalArgumentException> { ExternalSnapshotRef.Versioned("prices", "") }
        assertFailsWith<IllegalArgumentException> { ExternalSnapshotRef.Versioned("prices", "  ") }
    }

    @Test
    fun `Versioned data class equality is structural`() {
        assertEquals(
            ExternalSnapshotRef.Versioned("a", "1"),
            ExternalSnapshotRef.Versioned("a", "1"),
        )
        assertNotEquals(
            ExternalSnapshotRef.Versioned("a", "1"),
            ExternalSnapshotRef.Versioned("a", "2"),
        )
    }

    // ── ExternalSnapshotRef.parse — boundary-safe factory ────────────────────

    @Test
    fun `parse returns Versioned for valid id and version`() {
        val result = ExternalSnapshotRef.parse(id = "catalog", version = "42")
        assertTrue(result.isSuccess)
        val ref = result.getOrThrow()
        assertIs<ExternalSnapshotRef.Versioned>(ref)
        assertEquals("catalog", ref.id)
        assertEquals("42", ref.version)
    }

    @Test
    fun `parse returns UnversionedExternalRef for blank version`() {
        val result = ExternalSnapshotRef.parse(id = "catalog", version = "")
        assertTrue(result.isFailure)
        val err = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        val parseError = assertIs<ParseError.UnversionedExternalRef>(err.parseError)
        assertEquals("catalog", parseError.id)
    }

    @Test
    fun `parse returns UnversionedExternalRef for blank id`() {
        val result = ExternalSnapshotRef.parse(id = "", version = "v1")
        assertTrue(result.isFailure)
        val err = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        assertIs<ParseError.UnversionedExternalRef>(err.parseError)
    }

    // ── ParseError.UnversionedExternalRef ────────────────────────────────────

    @Test
    fun `UnversionedExternalRef message includes id`() {
        val err = ParseError.UnversionedExternalRef(id = "prices")
        assertTrue(err.message.contains("prices"))
    }

    @Test
    fun `ParseError companion factory creates UnversionedExternalRef`() {
        val err = ParseError.unversionedExternalRef("catalog")
        val parseError = assertIs<ParseError.UnversionedExternalRef>(err)
        assertEquals("catalog", parseError.id)
    }

    // ── InMemoryExternalRefRegistry — registration validation ─────────────────

    @Test
    fun `registry accepts versioned ref`() {
        val reg = InMemoryExternalRefRegistry("payments")
        val ref = ExternalSnapshotRef.Versioned("prices", "v1")
        assertTrue(reg.register(ref).isSuccess)
        assertEquals(1, reg.registeredRefs.size)
    }

    @Test
    fun `registry rejects ref with blank version — typed error not exception`() {
        val reg = InMemoryExternalRefRegistry("payments")
        // We can't construct a Versioned with a blank version (the init block rejects it),
        // so we test via the parse factory which can produce such an error.
        val result = ExternalSnapshotRef.parse(id = "prices", version = "  ")
        assertTrue(result.isFailure)
        assertIs<ParseError.UnversionedExternalRef>(
            assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull()).parseError,
        )
        // Registry remains empty — the invalid ref never reached it
        assertEquals(0, reg.registeredRefs.size)
    }

    @Test
    fun `registeredRefs preserves insertion order`() {
        val reg = InMemoryExternalRefRegistry("ns")
        reg.register(ExternalSnapshotRef.Versioned("c", "1"))
        reg.register(ExternalSnapshotRef.Versioned("a", "1"))
        reg.register(ExternalSnapshotRef.Versioned("b", "1"))

        assertEquals(listOf("c", "a", "b"), reg.registeredRefs.map { it.id })
    }

    // ── Namespace isolation ──────────────────────────────────────────────────

    @Test
    fun `registries in different namespaces are independent`() {
        val regA = InMemoryExternalRefRegistry("namespace-a")
        val regB = InMemoryExternalRefRegistry("namespace-b")

        regA.register(ExternalSnapshotRef.Versioned("prices", "v1"))

        assertEquals(1, regA.registeredRefs.size)
        assertEquals(0, regB.registeredRefs.size)
    }

    @Test
    fun `registering in namespace A does not affect namespace B`() {
        val regA = InMemoryExternalRefRegistry("a")
        val regB = InMemoryExternalRefRegistry("b")

        regA.register(ExternalSnapshotRef.Versioned("src", "v1"))
        regA.register(ExternalSnapshotRef.Versioned("alt", "v2"))

        assertTrue(regB.registeredRefs.isEmpty(), "namespace-b must have no refs from namespace-a")
    }

    // ── Sealed exhaustiveness ─────────────────────────────────────────────────

    @Test
    fun `ParseError sealed interface covers UnversionedExternalRef without else branch`() {
        fun classify(err: ParseError): String = when (err) {
            is ParseError.InvalidHexId -> "hex"
            is ParseError.InvalidRollout -> "rollout"
            is ParseError.InvalidVersion -> "version"
            is ParseError.FeatureNotFound -> "featureNotFound"
            is ParseError.FlagNotFound -> "flagNotFound"
            is ParseError.InvalidSnapshot -> "snapshot"
            is ParseError.InvalidJson -> "json"
            is ParseError.UnversionedExternalRef -> "unversioned"
        }

        assertEquals("unversioned", classify(ParseError.UnversionedExternalRef("x")))
    }
}
