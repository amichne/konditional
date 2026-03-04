package io.amichne.konditional.rules.predicate

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.rules.targeting.Targeting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PredicateRegistryTest {

    private fun registry(ns: String = "payments") = InMemoryPredicateRegistry<Context>(namespaceId = ns)

    private fun truePredicate(): Targeting.Custom<Context> = Targeting.Custom(block = { true }, weight = 1)
    private fun falsePredicate(): Targeting.Custom<Context> = Targeting.Custom(block = { false }, weight = 1)

    // ── PredicateRef ─────────────────────────────────────────────────────────

    @Test
    fun `PredicateRef BuiltIn rejects blank id`() {
        assertFailsWith<IllegalArgumentException> { PredicateRef.BuiltIn("") }
        assertFailsWith<IllegalArgumentException> { PredicateRef.BuiltIn("   ") }
    }

    @Test
    fun `PredicateRef Registered rejects blank id or namespaceId`() {
        assertFailsWith<IllegalArgumentException> { PredicateRef.Registered("ns", "") }
        assertFailsWith<IllegalArgumentException> { PredicateRef.Registered("", "id") }
    }

    // ── PredicateRef ordering ────────────────────────────────────────────────

    @Test
    fun `BuiltIn sorts before Registered`() {
        val builtIn = PredicateRef.BuiltIn("z-last")
        val registered = PredicateRef.Registered("ns", "a-first")
        assertTrue(builtIn < registered)
        assertTrue(registered > builtIn)
    }

    @Test
    fun `BuiltIn refs are ordered lexicographically by id`() {
        val refs = listOf(
            PredicateRef.BuiltIn("c"),
            PredicateRef.BuiltIn("a"),
            PredicateRef.BuiltIn("b"),
        ).sorted()
        assertEquals(listOf("a", "b", "c"), refs.map { it.id })
    }

    @Test
    fun `Registered refs are ordered by namespaceId then id`() {
        val refs = listOf(
            PredicateRef.Registered("beta", "z"),
            PredicateRef.Registered("alpha", "b"),
            PredicateRef.Registered("alpha", "a"),
        ).sorted()
        assertEquals(
            listOf("alpha/a", "alpha/b", "beta/z"),
            refs.filterIsInstance<PredicateRef.Registered>().map { "${it.namespaceId}/${it.id}" },
        )
    }

    @Test
    fun `ordering is stable across repeated sorts`() {
        val refs = listOf(
            PredicateRef.Registered("ns", "b"),
            PredicateRef.BuiltIn("c"),
            PredicateRef.Registered("ns", "a"),
            PredicateRef.BuiltIn("a"),
        )
        assertEquals(refs.sorted(), refs.sorted())
    }

    // ── Registration ────────────────────────────────────────────────────────

    @Test
    fun `register stores predicate and resolve returns it`() {
        val reg = registry("payments")
        val ref = PredicateRef.Registered("payments", "is-premium")
        reg.register(ref, truePredicate())

        val result = reg.resolve(ref)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `registeredRefs returns refs in insertion order`() {
        val reg = registry("ns")
        reg.register(PredicateRef.Registered("ns", "c"), truePredicate())
        reg.register(PredicateRef.Registered("ns", "a"), truePredicate())
        reg.register(PredicateRef.Registered("ns", "b"), truePredicate())

        assertEquals(listOf("c", "a", "b"), reg.registeredRefs.map { it.id })
    }

    @Test
    fun `register rejects ref with mismatched namespaceId`() {
        val reg = registry("payments")
        assertFailsWith<IllegalArgumentException> {
            reg.register(PredicateRef.Registered("other-ns", "p"), truePredicate())
        }
    }

    @Test
    fun `register rejects duplicate id`() {
        val reg = registry("ns")
        val ref = PredicateRef.Registered("ns", "dup")
        reg.register(ref, truePredicate())
        assertFailsWith<IllegalStateException> {
            reg.register(ref, falsePredicate())
        }
    }

    // ── Resolution failures ──────────────────────────────────────────────────

    @Test
    fun `resolve returns UnknownPredicate for unregistered Registered ref`() {
        val reg = registry("ns")
        val unknownRef = PredicateRef.Registered("ns", "unknown")
        val result = reg.resolve(unknownRef)

        assertTrue(result.isFailure)
        val error = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        val parseError = assertIs<ParseError.UnknownPredicate>(error.parseError)
        assertEquals(unknownRef, parseError.ref)
    }

    @Test
    fun `resolve returns UnknownPredicate for BuiltIn ref (not in consumer registry)`() {
        val reg = registry("ns")
        val builtIn = PredicateRef.BuiltIn("core-predicate")
        val result = reg.resolve(builtIn)

        assertTrue(result.isFailure)
        val error = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        assertIs<ParseError.UnknownPredicate>(error.parseError)
    }

    // ── Namespace isolation ──────────────────────────────────────────────────

    @Test
    fun `registries in different namespaces are independent`() {
        val regA = registry("namespace-a")
        val regB = registry("namespace-b")

        regA.register(PredicateRef.Registered("namespace-a", "predicate"), truePredicate())

        assertEquals(1, regA.registeredRefs.size)
        assertEquals(0, regB.registeredRefs.size)
    }

    @Test
    fun `resolving against wrong namespace registry returns failure`() {
        val regA = registry("namespace-a")
        val refA = PredicateRef.Registered("namespace-a", "p")
        regA.register(refA, truePredicate())

        // regB cannot resolve a ref scoped to namespace-a
        val regB = registry("namespace-b")
        assertTrue(regB.resolve(refA).isFailure)
    }

    // ── ParseError.UnknownPredicate ──────────────────────────────────────────

    @Test
    fun `ParseError UnknownPredicate message contains ref`() {
        val ref = PredicateRef.Registered("ns", "missing")
        val error = ParseError.UnknownPredicate(ref)
        assertTrue(error.message.contains("missing"))
    }

    @Test
    fun `ParseError companion factory delegates to UnknownPredicate`() {
        val ref = PredicateRef.BuiltIn("core-check")
        val error = ParseError.UnknownPredicate(ref)
        val parseError = assertIs<ParseError.UnknownPredicate>(error)
        assertEquals(ref, parseError.ref)
    }
}
