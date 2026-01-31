package io.amichne.konditional.openfeature

import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Reason
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KonditionalOpenFeatureProviderTest {

    private object TestFlags : Namespace.TestNamespaceFacade("openfeature-provider") {
        val enabled by boolean<Context>(default = true)
        val title by string<Context>(default = "hello")
        val retries by integer<Context>(default = 3)
        val multiplier by double<Context>(default = 1.5)
    }

    private val provider = KonditionalOpenFeatureProvider(
        namespaceRegistry = TestFlags,
        contextMapper = TargetingKeyContextMapper(),
    )

    private val context = ImmutableContext("user-123")

    @Test
    fun `boolean evaluation returns default with reason`() {
        val evaluation = provider.getBooleanEvaluation("enabled", false, context)

        assertEquals(true, evaluation.value)
        assertEquals(Reason.DEFAULT.name, evaluation.reason)
        assertEquals(null, evaluation.errorCode)
    }

    @Test
    fun `missing flag returns error`() {
        val evaluation = provider.getBooleanEvaluation("missing", false, context)

        assertEquals(false, evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.FLAG_NOT_FOUND, evaluation.errorCode)
    }

    @Test
    fun `type mismatch returns error`() {
        val evaluation = provider.getStringEvaluation("enabled", "fallback", context)

        assertEquals("fallback", evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.TYPE_MISMATCH, evaluation.errorCode)
    }

    @Test
    fun `invalid context returns error`() {
        val invalidContext = ImmutableContext()
        val evaluation = provider.getBooleanEvaluation("enabled", false, invalidContext)

        assertEquals(false, evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.INVALID_CONTEXT, evaluation.errorCode)
    }
}
