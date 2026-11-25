package io.amichne.konditional.context

import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.MultiFact
import io.amichne.konditional.context.fact.OrderedFact
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
import kotlin.test.assertTrue

class ConditionTest {
    private enum class Zone { COLD_STORAGE, LOADING_DOCK }

    private interface HasTemperature {
        val temperature: OrderedFact<Int>
    }

    private interface HasZones {
        val zones: MultiFact<Zone>
    }

    private class WarehouseFacts : FactSchema(), HasTemperature, HasZones {
        override val temperature = ordered(
            "warehouse.celsius",
            FactDomain<Int>(
                encode = Int::toString,
                decode = { raw ->
                    raw.toIntOrNull()?.let { ParseResult.Success(it) }
                        ?: ParseResult.Failure(ParseError.invalidValue("warehouse.celsius", "Expected whole degrees"))
                },
            ),
            naturalOrder(),
        )
        override val zones = multi("warehouse.zones", FactDomain.enumeration(Zone.entries) { it.name })
    }

    private fun temperatureBetween(low: Int, high: Int): Condition<HasTemperature> = {
        allOf(temperature.atLeast(low), temperature.atMost(high))
    }

    private fun inZone(zone: Zone): Condition<HasZones> = { zones.oneOf(zone) }

    @Test
    fun `binding a group equals grouping the bound children in order`() {
        val facts = WarehouseFacts()
        val cold = inZone(Zone.COLD_STORAGE)
        val chilled = temperatureBetween(0, 4)

        val all: Condition<WarehouseFacts> = allOf(cold, chilled, cold)
        val any: Condition<WarehouseFacts> = anyOf(listOf(cold, chilled))
        val nested: Condition<WarehouseFacts> = allOf(any, not(all))

        assertEquals(allOf(cold(facts), chilled(facts), cold(facts)), all(facts))
        assertEquals(anyOf(cold(facts), chilled(facts)), any(facts))
        assertEquals(allOf(any(facts), not(all(facts))), nested(facts))
    }

    @Test
    fun `conditions with different requirements compose without an expected type`() {
        val facts = WarehouseFacts()
        val chilledDelivery = named("chilledDelivery", allOf(inZone(Zone.COLD_STORAGE), temperatureBetween(0, 4)))

        val expected = FactSpec.Named(
            "chilledDelivery",
            FactSpec.All(
                listOf(
                    FactSpec.OneOf("warehouse.zones", listOf("COLD_STORAGE")),
                    FactSpec.All(
                        listOf(
                            FactSpec.AtLeast("warehouse.celsius", "0"),
                            FactSpec.AtMost("warehouse.celsius", "4"),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(expected, chilledDelivery(facts).spec)
        assertEquals(3, chilledDelivery(facts).specificity())
    }

    @Test
    fun `one condition binds to each schema instance with that instance's descriptors`() {
        val author = WarehouseFacts()
        val consumer = WarehouseFacts()
        val policy: Condition<WarehouseFacts> = allOf(inZone(Zone.LOADING_DOCK), temperatureBetween(-5, 5))

        assertEquals(policy(author).spec, policy(consumer).spec)
        assertTrue(policy(author) in author)
        assertFalse(policy(author) in consumer)
        assertEquals(listOf(author.zones, author.temperature, author.temperature), consumer.foreignFacts(policy(author)))
    }

    @Test
    fun `an empty or unnamed composition fails where it is assembled`() {
        assertFailsWith<IllegalArgumentException> { allOf(emptyList<Condition<WarehouseFacts>>()) }
        assertFailsWith<IllegalArgumentException> { anyOf(emptyList<Condition<WarehouseFacts>>()) }
        assertFailsWith<IllegalArgumentException> { named("", inZone(Zone.COLD_STORAGE)) }
    }
}
