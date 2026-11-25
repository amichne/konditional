package io.amichne.konditional.json

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.fixtures.RetryPolicy
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
    fun exportRejectsAnAnonymousPredicateEvenAlongsideAFlatCondition() {
        val namespace = object : Namespace("anonymous-export") {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    platforms(Platform.IOS)
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
    fun exportRejectsAnyOfEvenWhenBothChildrenAreSerializable() {
        val namespace = object : Namespace("any-of-export") {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    anyOf {
                        platforms(Platform.IOS)
                        locales(AppLocale.CANADA)
                    }
                }
            }
        }
        val failure = assertFailsWith<KonditionalBoundaryFailure> { namespace.toJson() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
        assertTrue(failure.parseError.message.contains(namespace.checkout.id.toString()))
        assertFalse(namespace.checkout.evaluate(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_STATES)))
    }

    @Test
    fun exportRejectsRepeatedConditionsThatShareOneSnapshotField() {
        val namespace = object : Namespace("repeated-export") {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    platforms(Platform.IOS)
                    platforms(Platform.ANDROID)
                }
            }
        }
        val failure = assertFailsWith<KonditionalBoundaryFailure> { namespace.toJson() }
        assertIs<ParseError.InvalidSnapshot>(failure.parseError)
    }

    @Test
    fun flatConditionsRoundTripWhenDeclaredInADifferentOrderFromTheWireFields() {
        val namespace = object : Namespace("flat-export") {
            val checkout by boolean<TestContext>(default = false) {
                rule(true) {
                    platforms(Platform.IOS)
                    locales(AppLocale.CANADA)
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

        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(json))
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

        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(json))
        assertTrue(
            failure.error is ParseError.InvalidSnapshot || failure.error is ParseError.InvalidValue,
        )
        assertEquals(RetryPolicy(), namespace.retryPolicy.evaluate(TestContext()))
    }

    @Test
    fun contextualReloadReportsFailureAndKeepsTheWholeSnapshot() {
        val namespace = object : Namespace.TestNamespaceFacade("contextual-reload") {
            val other by integer<Context>(default = 7)
            val contextual by boolean<Context>(default = false) {
                rule {} yields { true }
            }
        }
        val context = TestContext()
        val before = namespace.currentSnapshot
        val exported = namespace.toJson()

        assertTrue(namespace.contextual.evaluate(context))
        val failure = assertIs<ParseResult.Failure>(namespace.fromJson(exported))
        assertIs<ParseError.InvalidSnapshot>(failure.error)
        assertTrue(failure.error.message.contains(namespace.contextual.id.toString()))
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
