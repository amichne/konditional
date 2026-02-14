@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateSafely
import io.amichne.konditional.api.explainSafely
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.runtime.load
import io.amichne.konditional.serialization.instance.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FeatureEvaluationSafetyTest {
    private val context =
        Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.parse("1.0.0").getOrThrow(),
            stableId = StableId.of("safe-eval-user"),
        )

    @Test
    fun `evaluateSafely returns success when feature exists`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("safe-eval-success") {
                val feature by boolean<Context>(default = false) {
                    enable { platforms(Platform.IOS) }
                }
            }

        val result = namespace.feature.evaluateSafely(context)

        assertIs<ParseResult.Success<Boolean>>(result)
        assertTrue(result.value)
    }

    @Test
    fun `evaluateSafely returns typed feature-not-found error when definition is absent`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("safe-eval-missing") {
                val feature by boolean<Context>(default = false)
            }
        namespace.load(Configuration(emptyMap()))

        val result = namespace.feature.evaluateSafely(context)

        assertIs<ParseResult.Failure>(result)
        val error = assertIs<ParseError.FeatureNotFound>(result.error)
        assertEquals(namespace.feature.id, error.key)
    }

    @Test
    fun `explainSafely returns typed feature-not-found error when definition is absent`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("safe-explain-missing") {
                val feature by boolean<Context>(default = false)
            }
        namespace.load(Configuration(emptyMap()))

        val result = namespace.feature.explainSafely(context)

        assertIs<ParseResult.Failure>(result)
        val error = assertIs<ParseError.FeatureNotFound>(result.error)
        assertEquals(namespace.feature.id, error.key)
    }

    @Test
    fun `evaluate keeps throwing behavior for missing features`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("unsafe-eval-missing") {
                val feature by boolean<Context>(default = false)
            }
        namespace.load(Configuration(emptyMap()))

        val error = assertFailsWith<IllegalStateException> {
            namespace.feature.evaluate(context)
        }

        assertTrue(error.message.orEmpty().contains("Flag not found"))
    }
}
