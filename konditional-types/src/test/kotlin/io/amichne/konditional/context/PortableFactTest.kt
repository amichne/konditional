package io.amichne.konditional.context

import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactExpression
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.context.fact.anyOf
import io.amichne.konditional.context.fact.named
import io.amichne.konditional.context.fact.not
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PortableFactTest {
    private enum class Rank { LOW, HIGH }

    private class RankFacts : FactSchema() {
        private val domain = FactDomain.enumeration(Rank.entries) { it.name }
        val rank = ordered("rank", domain, naturalOrder())
        val roles = multi("roles", domain)
        val status = single("status", domain)
    }

    private fun context(vararg assignments: io.amichne.konditional.context.fact.FactValue<*>): Context =
        object : Context {
            override val facts = Facts.of(*assignments)
        }

    private fun FactSchema.failure(spec: FactSpec): ParseError = assertIs<ParseResult.Failure>(resolve(spec)).error

    @Test
    fun `portable expressions expose immutable ordered terms`() {
        val facts = RankFacts()
        val low = facts.status.oneOf(Rank.LOW)
        val high = facts.status.oneOf(Rank.HIGH)
        val spec = assertIs<FactSpec.All>(allOf(low, high, high).spec)
        assertFailsWith<UnsupportedOperationException> { java.util.Collections.reverse(spec.terms) }
        assertEquals(listOf(low.spec, high.spec, high.spec), spec.terms)
    }

    @Test
    fun `enum domains reject incomplete or ambiguous vocabulary`() {
        assertFailsWith<IllegalArgumentException> { FactDomain.enumeration(listOf(Rank.LOW)) { it.name } }
        assertFailsWith<IllegalArgumentException> {
            FactDomain.enumeration(listOf(Rank.LOW, Rank.LOW, Rank.HIGH)) { it.name }
        }
        assertFailsWith<IllegalArgumentException> { FactDomain.enumeration(Rank.entries) { "same" } }
    }

    @Test
    fun `set membership and ordered comparisons preserve cardinality through schema resolution`() {
        val facts = RankFacts()
        val assignedRoles = mutableSetOf(Rank.HIGH, Rank.LOW)
        val context = context(facts.rank.value(Rank.HIGH), facts.roles.values(assignedRoles))
        assignedRoles.clear()
        val expression = allOf(
            facts.rank.atLeast(Rank.HIGH),
            anyOf(facts.roles.oneOf(Rank.LOW), facts.rank.atMost(Rank.LOW)),
        )
        val restored = assertIs<ParseResult.Success<*>>(facts.resolve(expression.spec))
        assertEquals(expression, restored.value)
        assertTrue(expression.matches(context))
        assertFalse(expression.matches(object : Context {}))
        assertEquals(2, expression.specificity())
        assertFailsWith<IllegalArgumentException> {
            Facts.of(facts.rank.value(Rank.HIGH), facts.rank.value(Rank.LOW))
        }
    }

    @Test
    fun `separate declarations never acquire identity through a shared wire id`() {
        val left = RankFacts()
        val right = RankFacts()
        val foreign = right.status.oneOf(Rank.HIGH)
        assertFalse(left.status.oneOf(Rank.HIGH).matches(context(right.status.value(Rank.HIGH))))
        assertFalse(foreign in left)
        assertEquals(listOf(right.status), left.foreignFacts(allOf(left.status.oneOf(Rank.LOW), foreign)))
        assertFailsWith<IllegalArgumentException> {
            object : FactSchema() {
                val first = single("status", FactDomain.enumeration(Rank.entries) { it.name })
                val second = single("status", FactDomain.enumeration(Rank.entries) { it.name })
            }
        }
        assertEquals(ParseError.UnknownFact("other"), left.failure(FactSpec.OneOf("other", listOf("HIGH"))))
        assertEquals(
            ParseError.EmptyFactMembership(left.status.key),
            left.failure(FactSpec.OneOf("status", emptyList())),
        )
        assertEquals(ParseError.UnorderedFact(left.status.key), left.failure(FactSpec.AtMost("status", "HIGH")))
        val invalid = assertIs<ParseError.InvalidFactOperand>(
            left.failure(FactSpec.OneOf("status", listOf("UNKNOWN"))),
        )
        assertEquals(left.status.key, invalid.key)
        assertIs<ParseError.InvalidValue>(invalid.cause)
    }

    @Test
    fun `an empty group is rejected when authored and when resolved`() {
        val facts = RankFacts()
        assertFailsWith<IllegalArgumentException> { allOf(emptyList<FactExpression>()) }
        assertFailsWith<IllegalArgumentException> { anyOf(emptyList<FactExpression>()) }
        assertEquals(ParseError.EmptyFactGroup("all"), facts.failure(FactSpec.All(emptyList())))
        assertEquals(ParseError.EmptyFactGroup("anyOf"), facts.failure(FactSpec.AnyOf(emptyList())))
    }

    @Test
    fun `negation matches only when the context supplies the fact`() {
        val facts = RankFacts()
        val notHigh = not(facts.status.oneOf(Rank.HIGH))
        assertTrue(notHigh.matches(context(facts.status.value(Rank.LOW))))
        assertFalse(notHigh.matches(context(facts.status.value(Rank.HIGH))))
        assertFalse(notHigh.matches(context()))
        assertFalse(not(notHigh).matches(context()))
        assertEquals(notHigh, facts.status.noneOf(Rank.HIGH))

        // An assigned empty set is a known value, so "holds none of" matches it.
        val noHighRole = facts.roles.noneOf(Rank.HIGH)
        assertTrue(noHighRole.matches(context(facts.roles.values(emptySet()))))
        assertFalse(noHighRole.matches(context()))
    }

    @Test
    fun `absent facts stay unknown through groups`() {
        val facts = RankFacts()
        val supplied = context(facts.status.value(Rank.LOW))
        val statusLow = facts.status.oneOf(Rank.LOW)
        val rankHigh = facts.rank.atLeast(Rank.HIGH)

        // One known-true branch settles a disjunction. A conjunction needs every term known.
        assertTrue(anyOf(rankHigh, statusLow).matches(supplied))
        assertFalse(allOf(rankHigh, statusLow).matches(supplied))
        assertFalse(not(allOf(rankHigh, statusLow)).matches(supplied))
        // A known-false term settles a conjunction, so its negation matches.
        assertTrue(not(allOf(rankHigh, facts.status.oneOf(Rank.HIGH))).matches(supplied))
        assertFalse(not(anyOf(rankHigh, facts.status.oneOf(Rank.HIGH))).matches(supplied))
    }

    @Test
    fun `specificity counts distinct comparisons regardless of structure`() {
        val facts = RankFacts()
        val a = facts.status.oneOf(Rank.LOW)
        val b = facts.rank.atLeast(Rank.HIGH)
        val c = facts.roles.oneOf(Rank.HIGH)

        assertEquals(2, allOf(a, b).specificity())
        assertEquals(2, allOf(a, b, a).specificity())
        assertEquals(2, allOf(a, allOf(a, b)).specificity())
        assertEquals(2, allOf(named("first", a), named("policy", allOf(b, a))).specificity())
        assertEquals(3, allOf(allOf(a, b), allOf(b, c)).specificity())
        assertEquals(2, anyOf(a, allOf(a, b)).specificity())
        assertEquals(1, not(a).specificity())
        assertEquals(1, named("alone", a).specificity())
        // An equal comparison written twice is one requirement. A different operand is another.
        assertEquals(1, allOf(facts.status.oneOf(Rank.LOW), facts.status.oneOf(Rank.LOW)).specificity())
        assertEquals(2, allOf(facts.status.oneOf(Rank.LOW), facts.status.oneOf(Rank.HIGH)).specificity())
    }

    @Test
    fun `a label survives resolution and changes neither matching nor specificity`() {
        val facts = RankFacts()
        val plain = allOf(facts.status.oneOf(Rank.LOW), not(facts.rank.atLeast(Rank.HIGH)))
        val labeled = named("junior", plain)
        val context = context(facts.status.value(Rank.LOW), facts.rank.value(Rank.LOW))

        assertEquals(FactSpec.Named("junior", plain.spec), labeled.spec)
        assertEquals(plain.matches(context), labeled.matches(context))
        assertEquals(plain.specificity(), labeled.specificity())
        assertEquals(labeled, assertIs<ParseResult.Success<*>>(facts.resolve(labeled.spec)).value)
        assertFailsWith<IllegalArgumentException> { named(" ", plain) }
        assertEquals(ParseError.BlankFactLabel, facts.failure(FactSpec.Named("", plain.spec)))
    }
}
