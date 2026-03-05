package io.amichne.konditional.core

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.PredicateId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NamespacePredicateRegistryTest {

    private fun truePredicate(): Targeting.Custom<Context> =
        Targeting.Custom(block = { true }, weight = 1)

    @Test
    fun `predicates registry namespace matches namespace id`() {
        val namespace = Namespace("payments")

        assertEquals(namespace.id, namespace.predicates<Context>().namespaceId)
    }

    @Test
    fun `registering via namespace predicates allows resolution`() {
        val namespace = Namespace("payments")
        val registry = namespace.predicates<Context>()
        val ref = PredicateRef.Registered(namespace.id, PredicateId("is-premium"))
        val predicate = truePredicate()

        registry.register(ref, predicate)

        val resolved = registry.resolve(ref).getOrThrow()
        assertSame(predicate, resolved)
    }

    @Test
    fun `predicate registrations are isolated per namespace`() {
        val namespaceA = Namespace("payments-a")
        val namespaceB = Namespace("payments-b")
        val refInA = PredicateRef.Registered(namespaceA.id, PredicateId("is-premium"))

        namespaceA.predicates<Context>().register(refInA, truePredicate())

        assertTrue(namespaceA.predicates<Context>().resolve(refInA).isSuccess)
        assertTrue(namespaceB.predicates<Context>().resolve(refInA).isFailure)
    }

    @Test
    fun `registeredRefs preserve insertion order via namespace registry`() {
        val namespace = Namespace("order-check")
        val registry = namespace.predicates<Context>()
        val first = PredicateRef.Registered(namespace.id, PredicateId("c"))
        val second = PredicateRef.Registered(namespace.id, PredicateId("a"))
        val third = PredicateRef.Registered(namespace.id, PredicateId("b"))

        registry.register(first, truePredicate())
        registry.register(second, truePredicate())
        registry.register(third, truePredicate())

        assertEquals(listOf("c", "a", "b"), registry.registeredRefs.map { it.id.value })
    }

    @Test
    fun `test namespace facade exposes independent predicate registry`() {
        val facadeA = object : Namespace.TestNamespaceFacade("facade-a") {}
        val facadeB = object : Namespace.TestNamespaceFacade("facade-b") {}
        val refInA = PredicateRef.Registered(facadeA.id, PredicateId("is-beta"))

        facadeA.predicates<Context>().register(refInA, truePredicate())

        assertEquals(facadeA.id, facadeA.predicates<Context>().namespaceId)
        assertEquals(facadeB.id, facadeB.predicates<Context>().namespaceId)
        assertTrue(facadeA.predicates<Context>().resolve(refInA).isSuccess)
        assertTrue(facadeB.predicates<Context>().resolve(refInA).isFailure)
    }

    @Test
    fun `resolving unregistered ref returns ParseError UnknownPredicate`() {
        val namespace = Namespace("unknown-predicate")
        val missingRef = PredicateRef.Registered(namespace.id, PredicateId("missing"))

        val result = namespace.predicates<Context>().resolve(missingRef)

        assertTrue(result.isFailure)
        val boundaryFailure = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        val parseError = assertIs<ParseError.UnknownPredicate>(boundaryFailure.parseError)
        assertEquals(missingRef, parseError.ref)
    }
}
