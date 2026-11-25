package io.amichne.konditional.json

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.fixtures.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.fixtures.Platform
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.fixtures.RetryPolicy
import io.amichne.konditional.fixtures.TestFacts
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson
import io.amichne.konditional.values.FeatureId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NamespaceJsonTest {
    @Test
    fun zeroPercentRolloutPreservesBothAllowlistsAcrossSnapshotReload() {
        val flagUser = StableId.of("flag-tester")
        val ruleUser = StableId.of("rule-tester")
        val namespace = object : Namespace("rollout-round-trip") {
            val enabled by boolean<TestContext>(default = false) {
                allowlist(flagUser)
                rule(true) {
                    rollout { 0 }
                    allowlist(ruleUser)
                }
            }
        }

        val json = namespace.toJson()
        assertTrue("\"rollout\"" in json)
        assertTrue("\"rolloutAllowlist\"" in json)
        assertIs<ParseResult.Success<*>>(namespace.fromJson(json))
        assertTrue(namespace.enabled.evaluate(TestContext(stableId = flagUser)))
        assertTrue(namespace.enabled.evaluate(TestContext(stableId = ruleUser)))
        assertFalse(namespace.enabled.evaluate(TestContext(stableId = StableId.of("other"))))
    }

    @Test
    fun flatPayloadCannotSilentlyIgnorePortableConditions() {
        val namespace = object : Namespace("flat-with-conditions") {
            val enabled by boolean<Context>(default = false)
        }
        val before = namespace.currentSnapshot
        val json = """
            {"flags":[{"key":"${namespace.enabled.id.plainId}",
              "defaultValue":{"type":"BOOLEAN","value":false},
              "rules":[{"value":{"type":"BOOLEAN","value":true},
                        "conditions":[{"expression":{"anyOf":[]}}]}]}]}
        """.trimIndent()

        assertIs<ParseResult.Failure>(namespace.fromJson(json))
        assertSame(before, namespace.currentSnapshot)
    }

    @Test
    fun invalidExpressionsAndUnsupportedFormatsKeepTheInstalledSnapshot() {
        val namespace = object : Namespace("malformed-expression", TestFacts.schema) {
            val enabled by boolean<Context>(default = false)
        }
        val before = namespace.currentSnapshot
        fun snapshot(ruleFields: String): String = snapshotDocument("""
            {"flags":[{"key":"${namespace.enabled.id.plainId}",
             "defaultValue":{"type":"BOOLEAN","value":true},
             "rules":[{"value":{"type":"BOOLEAN","value":true},$ruleFields}]}]}
        """.trimIndent())
        val valid = snapshot(""""conditions":[]""")
        val malformed = listOf(
            valid.replace("konditional/facts-v1", "konditional/facts-v2"),
            snapshot(""""conditions":[],"platforms":["IOS"]"""),
            snapshot(""""conditions":[],"conditions":[{"all":[]}]"""),
            snapshot(""""conditions":[{"unknown":[]}]"""),
            snapshot(""""conditions":[{"all":[],"anyOf":[]}]"""),
            snapshot(""""conditions":[{"oneOf":{"fact":"sensor","operands":[7]}}]"""),
            snapshot(""""conditions":[{"atMost":{"fact":"sensor","operand":"4","extra":0}}]"""),
            snapshot(""""conditions":[null]"""),
            snapshot(""""conditions":[{"all":[null]}]"""),
            snapshot(""""conditions":[{"anyOf":null}]"""),
            snapshot(""""conditions":null"""),
            snapshot(""""conditions":[],"ruleId":"bad-rule-id""""),
            snapshot(""""conditions":[],"rollout":101"""),
            snapshot(""""conditions":[],"rollout":"20""""),
            snapshot(""""conditions":[],"rolloutAllowlist":[null]"""),
            snapshot(""""conditions":[],"rolloutAllowlist":[10]"""),
            valid.replace("""{"key":""", """null,{"key":"""),
            valid.replace("""{"value":{"type":"BOOLEAN","value":true},"conditions"""", """null,{"value":{"type":"BOOLEAN","value":true},"conditions""""),
            snapshot(""""note":"missing conditions""""),
            valid + "{}",
        )
        for (json in malformed) {
            assertIs<ParseResult.Failure>(namespace.fromJson(json), json)
            assertSame(before, namespace.currentSnapshot)
        }
    }

    @Test
    fun exportRejectsAnAnonymousPredicateEvenAlongsideAPortableCondition() {
        val namespace = object : Namespace("anonymous-export", TestFacts.schema) {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    match(TestFacts.platform.oneOf(Platform.IOS))
                    require { locale == AppLocale.CANADA }
                }
            }
        }
        val before = namespace.currentSnapshot
        val failure = assertFailsWith<KonditionalBoundaryFailure> { namespace.toJson() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
        assertTrue(failure.parseError.message.contains(namespace.checkout.id.toString()))
        assertSame(before, namespace.currentSnapshot)
        assertFalse(namespace.checkout.evaluate(TestContext(locale = AppLocale.UNITED_STATES)))
        assertTrue(namespace.checkout.evaluate(TestContext(locale = AppLocale.CANADA)))
    }

    @Test
    fun anyOfWithPortableChildrenRoundTrips() {
        val namespace = object : Namespace("any-of-export", TestFacts.schema) {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    anyOf {
                        match(TestFacts.platform.oneOf(Platform.IOS))
                        match(TestFacts.locale.oneOf(AppLocale.CANADA))
                    }
                }
            }
        }
        assertIs<ParseResult.Success<*>>(namespace.fromJson(namespace.toJson()))
        assertTrue(namespace.checkout.evaluate(TestContext(platform = Platform.ANDROID, locale = AppLocale.CANADA)))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_STATES)))
    }

    @Test
    fun repeatedConditionsKeepTheirConjunctionAfterReload() {
        val namespace = object : Namespace("repeated-export", TestFacts.schema) {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    match(TestFacts.platform.oneOf(Platform.IOS))
                    match(TestFacts.platform.oneOf(Platform.ANDROID))
                }
            }
        }
        assertIs<ParseResult.Success<*>>(namespace.fromJson(namespace.toJson()))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.IOS)))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.ANDROID)))
    }

    @Test
    fun factConditionsRoundTripInDeclarationOrder() {
        val namespace = object : Namespace("flat-export", TestFacts.schema) {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    match(TestFacts.platform.oneOf(Platform.IOS))
                    match(TestFacts.locale.oneOf(AppLocale.CANADA))
                }
            }
        }
        assertIs<ParseResult.Success<*>>(namespace.fromJson(namespace.toJson()))
        assertTrue(namespace.checkout.evaluate(TestContext(platform = Platform.IOS, locale = AppLocale.CANADA)))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.IOS, locale = AppLocale.UNITED_STATES)))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.ANDROID, locale = AppLocale.CANADA)))
    }

    private enum class Theme {
        LIGHT,
        DARK,
    }

    @Test
    fun strictRoundTripPreservesPrimitiveEnumAndKonstrainedValues() {
        val namespace = object : Namespace.TestNamespaceFacade("json-roundtrip") {
            val enabled by boolean<Context>(default = false)
            val theme by enum<Theme, Context>(default = Theme.LIGHT)
            val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy())
        }

        namespace.load(
            Configuration(
                flags = mapOf(
                    namespace.enabled to FlagDefinition(feature = namespace.enabled, bounds = emptyList(), defaultValue = true),
                    namespace.theme to FlagDefinition(feature = namespace.theme, bounds = emptyList(), defaultValue = Theme.DARK),
                    namespace.retryPolicy to FlagDefinition(
                        feature = namespace.retryPolicy,
                        bounds = emptyList(),
                        defaultValue = RetryPolicy(maxAttempts = 7, backoffMs = 250.0, enabled = false, mode = "linear"),
                    ),
                ),
                metadata = ConfigurationMetadata(version = "v1"),
            ),
        )

        val json = namespace.toJson()
        val result = namespace.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        assertEquals(true, namespace.enabled.evaluate(TestContext()))
        assertEquals(Theme.DARK, namespace.theme.evaluate(TestContext()))
        assertEquals(RetryPolicy(maxAttempts = 7, backoffMs = 250.0, enabled = false, mode = "linear"), namespace.retryPolicy.evaluate(TestContext()))
    }

    @Test
    fun unknownFeatureKeyFailsStrictlyAndLeavesCurrentStateUntouched() {
        val namespace = object : Namespace.TestNamespaceFacade("json-strict") {
            val enabled by boolean<Context>(default = false)
        }

        val previous = namespace.enabled.evaluate(TestContext())
        val unknownKey = FeatureId.create(namespace.id, "missing-flag")
        val json =
            """
            {
              "flags": [
                {
                  "key": "${unknownKey.plainId}",
                  "defaultValue": { "type": "BOOLEAN", "value": true }
                }
              ]
            }
            """.trimIndent()

        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(snapshotDocument(json)))
        assertIs<ParseError.FeatureNotFound>(failure.error)
        assertEquals(previous, namespace.enabled.evaluate(TestContext()))
    }

    @Test
    fun invalidKonstrainedPayloadFailsStrictly() {
        val namespace = object : Namespace.TestNamespaceFacade("json-konstrained") {
            val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy())
        }

        val json =
            """
            {
              "flags": [
                {
                  "key": "${namespace.retryPolicy.id.plainId}",
                  "defaultValue": {
                    "type": "DATA_CLASS",
                    "value": {
                      "maxAttempts": "oops",
                      "backoffMs": 1000.0,
                      "enabled": true,
                      "mode": "exp"
                    },
                    "dataClassName": "${RetryPolicy::class.java.name}"
                  }
                }
              ]
            }
            """.trimIndent()

        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(snapshotDocument(json)))
        assertTrue(
            failure.error is ParseError.InvalidSnapshot || failure.error is ParseError.InvalidValue,
        )
        assertEquals(RetryPolicy(), namespace.retryPolicy.evaluate(TestContext()))
    }

    @Test
    fun contextualExportReportsFailureAndKeepsTheWholeSnapshot() {
        val namespace = object : Namespace.TestNamespaceFacade("contextual-reload") {
            val other by integer<Context>(default = 7)
            val contextual by boolean<Context>(default = false) {
                rule {} yields { true }
            }
        }
        val context = TestContext()
        val before = namespace.currentSnapshot
        val failure = assertFailsWith<KonditionalBoundaryFailure> { namespace.toJson() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
        assertTrue(failure.parseError.message.contains(namespace.contextual.id.toString()))
        assertSame(before, namespace.currentSnapshot)
        assertTrue(namespace.contextual.evaluate(context))
        assertEquals(7, namespace.other.evaluate(context))
    }

    @Test
    fun malformedJsonFailsWithTypedBoundaryError() {
        val namespace = object : Namespace.TestNamespaceFacade("json-invalid") {
            val enabled by boolean<Context>(default = false)
        }

        val failure = assertIs<ParseResult.Failure>(namespace.fromJson("{ not-json"))

        assertIs<ParseError.InvalidJson>(failure.error)
        assertEquals(false, namespace.enabled.evaluate(TestContext()))
    }
}

internal fun snapshotDocument(snapshot: String): String =
    """{"format":"konditional/facts-v1","snapshot":$snapshot}"""
