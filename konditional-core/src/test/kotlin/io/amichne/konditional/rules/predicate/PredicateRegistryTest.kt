package io.amichne.konditional.rules.predicate

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.rules.predicate.PredicateRef.BuiltIn
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.PredicateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PredicateRegistryTest {

    private fun registry(namespaceId: String = "ns") =
        InMemoryPredicateRegistry<Context>(namespaceId = NamespaceId(namespaceId))

    private fun truePredicate(): Targeting.Custom<Context> = Targeting.Custom(block = { true }, weight = 1)
    private fun falsePredicate(): Targeting.Custom<Context> = Targeting.Custom(block = { false }, weight = 1)

    // ── PredicateRef ─────────────────────────────────────────────────────────

    @Test
    fun `PredicateRef BuiltIn rejects blank id`() {
        assertFailsWith<IllegalArgumentException> { BuiltIn(PredicateId("")) }
        assertFailsWith<IllegalArgumentException> { BuiltIn(PredicateId("   ")) }
    }

    @Test
    fun `PredicateRef Registered rejects blank id or namespaceId`() {
        assertFailsWith<IllegalArgumentException> { PredicateRef.Registered(NamespaceId("ns"), PredicateId("")) }
        assertFailsWith<IllegalArgumentException> { PredicateRef.Registered(NamespaceId(""), PredicateId("id")) }
    }

    // ── PredicateRef ordering ────────────────────────────────────────────────

    @Test
    fun `BuiltIn sorts before Registered`() {
        val builtIn = BuiltIn(PredicateId("z-last"))
        val registered = PredicateRef.Registered(NamespaceId("ns"), PredicateId("a-first"))
        assertTrue(builtIn < registered)
        assertTrue(registered > builtIn)
    }

    @Test
    fun `BuiltIn refs are ordered lexicographically by id`() {
        val refs = listOf(
            BuiltIn(PredicateId("c")),
            BuiltIn(PredicateId("a")),
            BuiltIn(PredicateId("b")),
        ).sorted()
        assertEquals(listOf("a", "b", "c"), refs.map { it.id.value })
    }

    @Test
    fun `Registered refs are ordered by namespaceId then id`() {
        val refs = listOf(
            PredicateRef.Registered(NamespaceId("beta"), PredicateId("z")),
            PredicateRef.Registered(NamespaceId("alpha"), PredicateId("b")),
            PredicateRef.Registered(NamespaceId("alpha"), PredicateId("a")),
        ).sorted()
        assertEquals(
            listOf("alpha/a", "alpha/b", "beta/z"),
            refs.filterIsInstance<PredicateRef.Registered>().map { "${it.namespaceId.value}/${it.id.value}" },
        )
    }

    @Test
    fun `ordering is stable across repeated sorts`() {
        val refs = listOf(
            PredicateRef.Registered(NamespaceId("ns"), PredicateId("b")),
            BuiltIn(PredicateId("c")),
            PredicateRef.Registered(NamespaceId("ns"), PredicateId("a")),
            BuiltIn(PredicateId("a")),
        )
        assertEquals(refs.sorted(), refs.sorted())
    }

    // ── Registration ────────────────────────────────────────────────────────

    @Test
    fun `register stores predicate and resolve returns it`() {
        val reg = registry()
        val ref = PredicateRef.Registered(NamespaceId("ns"), PredicateId("is-premium"))
        reg.register(ref, truePredicate())

        val result = reg.resolve(ref)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `registeredRefs returns refs in insertion order`() {
        val reg = registry()
        reg.register(PredicateRef.Registered(NamespaceId("ns"), PredicateId("c")), truePredicate())
        reg.register(PredicateRef.Registered(NamespaceId("ns"), PredicateId("a")), truePredicate())
        reg.register(PredicateRef.Registered(NamespaceId("ns"), PredicateId("b")), truePredicate())

        assertEquals(listOf("c", "a", "b"), reg.registeredRefs.map { it.id.value })
    }

    @Test
    fun `register rejects ref with mismatched namespaceId`() {
        val reg = registry()
        assertFailsWith<IllegalArgumentException> {
            reg.register(PredicateRef.Registered(NamespaceId("other-ns"), PredicateId("p")), truePredicate())
        }
    }

    @Test
    fun `register rejects duplicate id`() {
        val reg = registry()
        val ref = PredicateRef.Registered(NamespaceId("ns"), PredicateId("dup"))
        reg.register(ref, truePredicate())
        assertFailsWith<IllegalStateException> {
            reg.register(ref, falsePredicate())
        }
    }

    @Test
    @OptIn(KonditionalInternalApi::class)
    fun `registerOrReplace updates existing id`() {
        val reg = registry()
        val ref = PredicateRef.Registered(NamespaceId("ns"), PredicateId("replaceable"))
        reg.register(ref, truePredicate())

        reg.registerOrReplace(ref, falsePredicate())

        assertFalse(reg.resolve(ref).getOrThrow().matches(object : Context {}))
    }

    // ── Resolution failures ──────────────────────────────────────────────────

    @Test
    fun `resolve returns UnknownPredicate for unregistered Registered ref`() {
        val reg = registry()
        val unknownRef = PredicateRef.Registered(NamespaceId("ns"), PredicateId("unknown"))
        val result = reg.resolve(unknownRef)

        assertTrue(result.isFailure)
        val error = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        val parseError = assertIs<ParseError.UnknownPredicate>(error.parseError)
        assertEquals(unknownRef, parseError.ref)
    }

    @Test
    fun `resolve returns UnknownPredicate for BuiltIn ref (not in consumer registry)`() {
        val reg = registry()
        val builtIn = BuiltIn(PredicateId("core-predicate"))
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

        regA.register(PredicateRef.Registered(NamespaceId("namespace-a"), PredicateId("predicate")), truePredicate())

        assertEquals(1, regA.registeredRefs.size)
        assertEquals(0, regB.registeredRefs.size)
    }

    @Test
    fun `resolving against wrong namespace registry returns failure`() {
        val regA = registry("namespace-a")
        val refA = PredicateRef.Registered(NamespaceId("namespace-a"), PredicateId("p"))
        regA.register(refA, truePredicate())

        // regB cannot resolve a ref scoped to namespace-a
        val regB = registry("namespace-b")
        assertTrue(regB.resolve(refA).isFailure)
    }

    // ── ParseError.UnknownPredicate ──────────────────────────────────────────

    @Test
    fun `ParseError UnknownPredicate message contains ref`() {
        val ref = PredicateRef.Registered(NamespaceId("ns"), PredicateId("missing"))
        val error = ParseError.UnknownPredicate(ref)
        assertTrue(error.message.contains("missing"))
    }

    @Test
    fun `ParseError companion factory delegates to UnknownPredicate`() {
        val ref = BuiltIn(PredicateId("core-check"))
        val error = ParseError.UnknownPredicate(ref)
        val parseError = assertIs<ParseError.UnknownPredicate>(error)
        assertEquals(ref, parseError.ref)
    }
}
