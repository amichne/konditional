import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.OrderedFact
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.context.fact.named
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enableWhen
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.Decision
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.TargetingNode
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WarehouseRulesTest {
    private enum class Zone { COLD_STORAGE, LOADING_DOCK }

    @JvmInline
    private value class Celsius(val degrees: Int)

    /** A requirement that any schema with a temperature can meet. */
    private interface HasTemperature {
        val temperature: OrderedFact<Celsius>
    }

    private class WarehouseFacts : FactSchema(), HasTemperature {
        val zone = single("warehouse.zone", FactDomain.enumeration(Zone.entries) { it.name })
        override val temperature = ordered(
            "warehouse.celsius",
            FactDomain<Celsius>(
                encode = { it.degrees.toString() },
                decode = { raw ->
                    raw.toIntOrNull()?.let { ParseResult.Success(Celsius(it)) }
                        ?: ParseResult.Failure(ParseError.invalidValue("warehouse.celsius", "Expected whole degrees"))
                },
            ),
            compareBy(Celsius::degrees),
        )

        fun reading(location: Zone, degrees: Int): Context =
            Context(Facts.of(zone.value(location), temperature.value(Celsius(degrees))))
    }

    private class Warehouse(facts: WarehouseFacts, build: Boolean) : Namespace<WarehouseFacts>("warehouse", facts) {
        val acceptDelivery by boolean<Context>(default = false) {
            if (build) enableWhen(chilledDelivery)
        }
    }

    @Test
    fun portableWarehouseRuleSurvivesTransferToAnIndependentConsumer() {
        val author = WarehouseFacts()
        val source = Warehouse(author, build = true)
        val consumer = WarehouseFacts()
        val receiver = Warehouse(consumer, build = false)
        val accepted = consumer.reading(Zone.COLD_STORAGE, 4)
        val expected = source.acceptDelivery.explain(author.reading(Zone.COLD_STORAGE, 4))
        val rule = assertIs<Decision.Rule>(expected.decision).matched
        val expression = FactSpec.Named(
            "chilledDelivery",
            FactSpec.All(
                listOf(
                    FactSpec.OneOf("warehouse.zone", listOf("COLD_STORAGE")),
                    FactSpec.All(
                        listOf(
                            FactSpec.AtLeast("warehouse.celsius", "0"),
                            FactSpec.AtMost("warehouse.celsius", "4"),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(TargetingNode.All(listOf(TargetingNode.Facts(expression))), rule.targeting)
        assertEquals(3, rule.totalSpecificity)
        assertTrue(expected.value)
        assertFalse(receiver.acceptDelivery.evaluate(accepted))

        val json = source.toJson()
        assertIs<ParseResult.Success<*>>(receiver.fromJson(json))
        val restored = receiver.acceptDelivery.explain(accepted)
        assertTrue(restored.value)
        assertEquals(rule.rule, assertIs<Decision.Rule>(restored.decision).matched.rule)
        assertTrue(receiver.acceptDelivery.evaluate(consumer.reading(Zone.COLD_STORAGE, 0)))
        assertFalse(receiver.acceptDelivery.evaluate(consumer.reading(Zone.COLD_STORAGE, -1)))
        assertFalse(receiver.acceptDelivery.evaluate(consumer.reading(Zone.COLD_STORAGE, 5)))
        assertFalse(receiver.acceptDelivery.evaluate(consumer.reading(Zone.LOADING_DOCK, 4)))
        assertFalse(receiver.acceptDelivery.evaluate(Context()))

        val installed = receiver.currentSnapshot
        val unknownFact = json.replace("warehouse.zone", "warehouse.unknown")
        val badZone = json.replace("COLD_STORAGE", "UNDECLARED_ZONE")
        for (invalid in listOf(unknownFact, badZone)) {
            assertIs<ParseResult.Failure>(receiver.fromJson(invalid))
            assertSame(installed, receiver.currentSnapshot)
            assertTrue(receiver.acceptDelivery.evaluate(accepted))
        }
    }

    @Test
    fun aConditionThatCapturesAnotherInstancesFactFailsTheDeclaration() {
        val other = WarehouseFacts()
        val captured: Condition<WarehouseFacts> = { allOf(inZone(Zone.COLD_STORAGE)(this), other.temperature.atMost(Celsius(4))) }

        val failure = assertFailsWith<IllegalArgumentException> {
            object : Namespace<WarehouseFacts>("warehouse", WarehouseFacts()) {
                val acceptDelivery by boolean<Context>(default = false) { enableWhen(captured) }
            }
        }
        assertTrue("'warehouse.celsius'" in failure.message.orEmpty(), failure.message)
    }

    private companion object {
        fun inZone(required: Zone): Condition<WarehouseFacts> = { zone.oneOf(required) }

        fun temperatureBetween(low: Celsius, high: Celsius): Condition<HasTemperature> = {
            allOf(temperature.atLeast(low), temperature.atMost(high))
        }

        val chilledDelivery: Condition<WarehouseFacts> = named(
            "chilledDelivery",
            allOf(inZone(Zone.COLD_STORAGE), temperatureBetween(Celsius(0), Celsius(4))),
        )
    }
}
