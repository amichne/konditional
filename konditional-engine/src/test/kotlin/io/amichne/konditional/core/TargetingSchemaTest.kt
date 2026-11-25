package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.rules.targeting.ContextAttribute
import io.amichne.konditional.rules.targeting.NamedPredicate
import io.amichne.konditional.rules.targeting.TargetingSchema
import io.amichne.konditional.rules.targeting.TargetingType
import io.amichne.konditional.rules.targeting.allOf
import io.amichne.konditional.rules.targeting.anyOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TargetingSchemaTest {
    private data class Buyer(val seats: Int) : Context

    @Test
    fun `predicate versions have independent behavior and canonical parameter types`() {
        val atLeast = NamedPredicate<Buyer, Int>("seats", 1, TargetingType.int) { minimum -> seats >= minimum }
        val greater = NamedPredicate<Buyer, Int>("seats", 2, TargetingType.int) { minimum -> seats > minimum }
        val schema = TargetingSchema(atLeast, greater)
        val context = Buyer(10)

        val first = schema.resolvePredicate("seats", 1, "10")
        assertTrue(first is ParseResult.Success)
        assertTrue(first.value.matches(context))
        val second = schema.resolvePredicate("seats", 2, "10")
        assertTrue(second is ParseResult.Success)
        assertFalse(second.value.matches(context))
        assertIs<ParseResult.Failure>(schema.resolvePredicate("seats", 3, "10"))
        assertIs<ParseResult.Failure>(schema.resolvePredicate("seats", 1, "+10"))
        assertFailsWith<IllegalArgumentException> { TargetingSchema(atLeast, atLeast) }
    }

    @Test
    fun `a matching key cannot substitute a foreign declaration inside a group`() {
        val allowed = ContextAttribute("seats", TargetingType.int, Buyer::seats)
        val foreign = ContextAttribute<Buyer, Int>("seats", TargetingType.int) { 100 }
        assertFailsWith<IllegalArgumentException> { TargetingSchema(allowed, foreign) }
        assertFailsWith<IllegalArgumentException> {
            object : Namespace("foreign-declaration") {
                val checkout by boolean<Buyer>(default = false) {
                    targetingSchema(TargetingSchema(allowed))
                    enable { targeting(anyOf(allowed.isOneOf(2), foreign.isOneOf(100))) }
                }
            }
        }
    }

    @Test
    fun `an authored predicate captures a snapshot of its argument`() {
        val accepted = mutableListOf(2, 3)
        val ints = TargetingType.custom<List<Int>>(
            encode = { values -> values.joinToString(",") },
            decode = { text ->
                val tokens = text.split(',')
                tokens.mapNotNull(String::toIntOrNull).takeIf { it.size == tokens.size }
            },
        )
        val among = NamedPredicate<Buyer, List<Int>>("seatCounts", 1, ints) { values -> seats in values }
        val seats = ContextAttribute("seats", TargetingType.int, Buyer::seats)
        val flags = object : Namespace("captured-argument") {
            val checkout by boolean<Buyer>(false) {
                targetingSchema(TargetingSchema(among, seats))
                enable { targeting(allOf(among(accepted), seats.isOneOf(2, 3, 4))) }
            }
        }

        accepted.clear()
        accepted.add(4)
        assertTrue(flags.checkout.evaluate(Buyer(2)))
        assertFalse(flags.checkout.evaluate(Buyer(4)))
    }
}
