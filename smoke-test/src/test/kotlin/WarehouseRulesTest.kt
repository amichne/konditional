import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.OrderedFact
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.context.fact.allOf
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.Decision
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.TargetingNode
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WarehouseRulesTest {
    private enum class Zone { COLD_STORAGE, LOADING_DOCK }

    @JvmInline
    private value class Celsius(val degrees: Int)

    private class WarehouseFacts {
        val zone = SingleFact("warehouse.zone", FactDomain.enumeration(Zone.entries) { it.name })
        val temperature = OrderedFact(
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
        val schema = FactSchema(zone, temperature)

        fun reading(location: Zone, degrees: Int): Context = object : Context {
            override val facts = Facts.of(zone.value(location), temperature.value(Celsius(degrees)))
        }
    }

    @Test
    fun portableWarehouseRuleSurvivesTransferToAnIndependentConsumer() {
        val author = WarehouseFacts()
        val source = object : Namespace("warehouse", factSchema = author.schema) {
            val acceptDelivery by boolean<Context>(default = false) {
                rule(true, allOf(author.zone.oneOf(Zone.COLD_STORAGE), author.temperature.atMost(Celsius(4))))
            }
        }
        val consumer = WarehouseFacts()
        val receiver = object : Namespace("warehouse", factSchema = consumer.schema) {
            val acceptDelivery by boolean<Context>(default = false)
        }
        val accepted = consumer.reading(Zone.COLD_STORAGE, 4)
        val expected = source.acceptDelivery.explain(author.reading(Zone.COLD_STORAGE, 4))
        val rule = assertIs<Decision.Rule>(expected.decision).matched
        val expression = FactSpec.All(listOf(
            FactSpec.OneOf("warehouse.zone", listOf("COLD_STORAGE")),
            FactSpec.AtMost("warehouse.celsius", "4"),
        ))
        assertEquals(TargetingNode.All(listOf(TargetingNode.Facts(expression))), rule.targeting)
        assertTrue(expected.value)
        assertFalse(receiver.acceptDelivery.evaluate(accepted))

        val json = source.toJson()
        assertIs<ParseResult.Success<*>>(receiver.fromJson(json))
        val restored = receiver.acceptDelivery.explain(accepted)
        assertTrue(restored.value)
        assertEquals(rule.rule, assertIs<Decision.Rule>(restored.decision).matched.rule)
        assertTrue(receiver.acceptDelivery.evaluate(consumer.reading(Zone.COLD_STORAGE, -1)))
        assertFalse(receiver.acceptDelivery.evaluate(consumer.reading(Zone.COLD_STORAGE, 5)))
        assertFalse(receiver.acceptDelivery.evaluate(consumer.reading(Zone.LOADING_DOCK, 4)))
        assertFalse(receiver.acceptDelivery.evaluate(object : Context {}))

        val installed = receiver.currentSnapshot
        val unknownFact = json.replace("warehouse.zone", "warehouse.unknown")
        val badZone = json.replace("COLD_STORAGE", "UNDECLARED_ZONE")
        for (invalid in listOf(unknownFact, badZone)) {
            assertIs<ParseResult.Failure>(receiver.fromJson(invalid))
            assertSame(installed, receiver.currentSnapshot)
            assertTrue(receiver.acceptDelivery.evaluate(accepted))
        }
    }
}
