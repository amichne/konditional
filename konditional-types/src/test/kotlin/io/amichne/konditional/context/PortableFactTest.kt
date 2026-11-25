package io.amichne.konditional.context

import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.MultiFact
import io.amichne.konditional.context.fact.OrderedFact
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.context.fact.anyOf
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

    @Test
    fun `portable expressions expose immutable ordered terms`() {
        val rank = SingleFact("rank", FactDomain.enumeration(Rank.entries) { it.name })
        val low = rank.oneOf(Rank.LOW)
        val high = rank.oneOf(Rank.HIGH)
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
        val domain = FactDomain.enumeration(Rank.entries) { it.name }
        val rank = OrderedFact("rank", domain, naturalOrder())
        val roles = MultiFact("roles", domain)
        val schema = FactSchema(rank, roles)
        val assignedRoles = mutableSetOf(Rank.HIGH, Rank.LOW)
        val context = object : Context {
            override val facts = Facts.of(rank.value(Rank.HIGH), roles.values(assignedRoles))
        }
        assignedRoles.clear()
        val expression = allOf(rank.atLeast(Rank.HIGH), anyOf(roles.oneOf(Rank.LOW), rank.atMost(Rank.LOW)))
        val restored = assertIs<ParseResult.Success<*>>(schema.resolve(expression.spec))
        assertEquals(expression, restored.value)
        assertTrue(expression.matches(context))
        assertFalse(expression.matches(object : Context {}))
        assertEquals(2, expression.specificity())
        assertFailsWith<IllegalArgumentException> { Facts.of(rank.value(Rank.HIGH), rank.value(Rank.LOW)) }
    }

    @Test
    fun `separate declarations never acquire identity through a shared wire id`() {
        val domain = FactDomain.enumeration(Rank.entries) { it.name }
        val left = SingleFact("status", domain)
        val right = SingleFact("status", domain)
        val leftSchema = FactSchema(left)
        val context = object : Context {
            override val facts = Facts.of(right.value(Rank.HIGH))
        }
        assertFalse(left.oneOf(Rank.HIGH).matches(context))
        assertFalse(leftSchema.contains(right.oneOf(Rank.HIGH)))
        assertFailsWith<IllegalArgumentException> { FactSchema(left, right) }
        fun failure(spec: FactSpec): ParseError = assertIs<ParseResult.Failure>(leftSchema.resolve(spec)).error
        assertEquals(ParseError.UnknownFact("other"), failure(FactSpec.OneOf("other", listOf("HIGH"))))
        assertEquals(ParseError.EmptyFactMembership(left.key), failure(FactSpec.OneOf("status", emptyList())))
        assertEquals(ParseError.UnorderedFact(left.key), failure(FactSpec.AtMost("status", "HIGH")))
        val invalid = assertIs<ParseError.InvalidFactOperand>(failure(FactSpec.OneOf("status", listOf("UNKNOWN"))))
        assertEquals(left.key, invalid.key)
        assertIs<ParseError.InvalidValue>(invalid.cause)
    }
}
