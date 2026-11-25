package io.amichne.konditional.serialization

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics.Decision
import io.amichne.konditional.rules.targeting.ContextAttribute
import io.amichne.konditional.rules.targeting.NamedPredicate
import io.amichne.konditional.rules.targeting.TargetingSchema
import io.amichne.konditional.rules.targeting.TargetingType
import io.amichne.konditional.rules.targeting.allOf
import io.amichne.konditional.rules.targeting.anyOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TargetingRoundTripTest {
    private enum class Plan { ENTERPRISE, TRIAL, PERSONAL }
    private data class Buyer(
        val plan: Plan,
        val seats: Int,
        val preview: Boolean = false,
        override val stableId: StableId = StableId.of("user-1"),
    ) : Context.StableIdContext

    private class Checkout(useSeatCount: Boolean, percentage: Int = 100) : Namespace("checkout") {
        val plan = ContextAttribute("plan", TargetingType.enum<Plan>(), Buyer::plan)
        val preview = ContextAttribute("preview", TargetingType.boolean, Buyer::preview)
        val hasSeats = NamedPredicate<Buyer, Int>("minimumSeats", version = 1, argumentType = TargetingType.int) {
            minimum -> seats >= minimum
        }
        val fields = TargetingSchema(plan, preview, hasSeats)

        val express by boolean<Buyer>(default = false) {
            targetingSchema(fields)
            enable {
                rampUp { percentage }
                if (useSeatCount) {
                    targeting(allOf(plan.isOneOf(Plan.ENTERPRISE, Plan.TRIAL), anyOf(hasSeats(10), preview.isOneOf(true))))
                } else {
                    targeting(plan.isOneOf(Plan.ENTERPRISE))
                }
            }
        }
    }

    @Test
    fun `offline targeting and fractional rollout use the same assignments`() {
        val publisher = Checkout(useSeatCount = true, percentage = 25)
        val client = Checkout(useSeatCount = false)
        assertIs<ParseResult.Success<*>>(client.fromJson(publisher.toJson()))

        // Independent SHA-256 vectors for the existing v1 salt and express flag key.
        val included = Buyer(Plan.ENTERPRISE, 10, stableId = StableId.of("user-4"))
        val outside = included.copy(stableId = StableId.of("user-1"))
        for (flags in listOf(publisher, client)) {
            val match = assertIs<Decision.Rule>(flags.express.explain(included).decision)
            assertEquals(2334, match.matched.bucket.bucket)
            val skip = assertIs<Decision.Default>(flags.express.explain(outside).decision)
            assertEquals(7970, skip.skippedByRollout?.bucket?.bucket)
            assertFalse(flags.express.evaluate(included.copy(plan = Plan.PERSONAL)))
        }
    }

    @Test
    fun `a fresh client binds new expressions to its own declarations and can export them again`() {
        val publisher = Checkout(useSeatCount = true)
        val offlineClient = Checkout(useSeatCount = false)
        val buyers = listOf(
            Buyer(Plan.ENTERPRISE, 9), Buyer(Plan.ENTERPRISE, 10),
            Buyer(Plan.TRIAL, 0, preview = true), Buyer(Plan.TRIAL, 11),
            Buyer(Plan.PERSONAL, 50, preview = true),
        )
        val expected = listOf(false, true, true, true, false)
        assertEquals(listOf(true, true, false, false, false), buyers.map { offlineClient.express.evaluate(it) })
        assertEquals(expected, buyers.map { publisher.express.evaluate(it) })

        val payload = publisher.toJson()
        assertIs<ParseResult.Success<*>>(offlineClient.fromJson(payload))
        assertEquals(expected, buyers.map { offlineClient.express.evaluate(it) })
        for (buyer in buyers) {
            assertEquals(publisher.express.explain(buyer).decision, offlineClient.express.explain(buyer).decision)
        }
        assertEquals(payload, offlineClient.toJson())

        val afterLoad = offlineClient.currentSnapshot
        for ((valid, invalid) in listOf(
            "\"minimumSeats\"" to "\"missingPredicate\"",
            "\"version\": 1" to "\"version\": 2",
            "\"argument\": \"10\"" to "\"argument\": \"ten\"",
            "\"argument\": \"10\"" to "\"argument\": \"010\"",
            "\"argument\": \"10\"" to "\"argument\": 10",
            "\"plan\"" to "\"unknownField\"",
            "\"TRIAL\"" to "\"UNKNOWN_PLAN\"",
            "\"op\": \"anyOf\"" to "\"op\": \"someFutureOperator\"",
            "\"op\": \"anyOf\"" to "\"op\": \"anyOf\", \"unexpected\": true",
            "\"targeting\":" to "\"unrecognizedTargeting\":",
            "\"targeting\":" to "\"locales\": [\"en\"], \"targeting\":",
            "\"targeting\":" to "\"locales\": null, \"targeting\":",
            "\"TARGETING_V1\"" to "\"STATIC\"",
        )) {
            assertTrue(valid in payload, valid)
            assertIs<ParseResult.Failure>(offlineClient.fromJson(payload.replace(valid, invalid)), invalid)
            assertSame(afterLoad, offlineClient.currentSnapshot, invalid)
        }
    }

    @Test
    fun `legacy flat targeting still loads in a client with new declarations`() {
        val client = Checkout(useSeatCount = true)
        val payload = """
            {"flags":[{"key":"feature::checkout::express","defaultValue":{"type":"BOOLEAN","value":false},
              "rules":[{"value":{"type":"BOOLEAN","value":true},"platforms":["IOS"]}]}]}
        """.trimIndent()
        assertIs<ParseResult.Success<*>>(client.fromJson(payload))
        assertFalse(client.express.evaluate(Buyer(Plan.ENTERPRISE, 10)))
    }

    @Test
    fun `a version expression preserves each kind of range through export`() {
        val flags = object : Namespace("versions") {
            val enabled by boolean<Context>(default = false)
        }
        val v2 = object : Context.VersionContext { override val appVersion = Version(2, 0, 0) }
        val v4 = object : Context.VersionContext { override val appVersion = Version(4, 0, 0) }
        for ((limits, enabledAt4) in listOf(
            "" to true,
            ",\"min\":\"1.0.0\"" to true,
            ",\"max\":\"3.0.0\"" to false,
            ",\"min\":\"1.0.0\",\"max\":\"3.0.0\"" to false,
        )) {
            val payload = """
                {"flags":[{"key":"feature::versions::enabled","defaultValue":{"type":"BOOLEAN","value":false},
                  "rules":[{"type":"TARGETING_V1","value":{"type":"BOOLEAN","value":true},
                    "targeting":{"op":"anyOf","operands":[{"op":"version"$limits}]}}]}]}
            """.trimIndent()
            assertIs<ParseResult.Success<*>>(flags.fromJson(payload))
            val decision = flags.enabled.explain(v2).decision
            assertIs<ParseResult.Success<*>>(flags.fromJson(flags.toJson()))
            assertTrue(flags.enabled.evaluate(v2))
            assertEquals(enabledAt4, flags.enabled.evaluate(v4))
            assertFalse(flags.enabled.evaluate(object : Context {}))
            assertEquals(decision, flags.enabled.explain(v2).decision)
        }
    }

    @Test
    fun `disjunction keeps its exclusions after snapshot reload`() {
        val flags = object : Namespace("clients") {
            val enabled by boolean<Context>(default = false) {
                enable { anyOf { platforms(Platform.IOS) } }
            }
        }
        val ios = object : Context.PlatformContext { override val platform = Platform.IOS }
        val unidentified = object : Context {}
        assertTrue(flags.enabled.evaluate(ios))
        assertFalse(flags.enabled.evaluate(unidentified))

        assertIs<ParseResult.Success<*>>(flags.fromJson(flags.toJson()))
        assertTrue(flags.enabled.evaluate(ios))
        assertFalse(flags.enabled.evaluate(unidentified))
    }
}
