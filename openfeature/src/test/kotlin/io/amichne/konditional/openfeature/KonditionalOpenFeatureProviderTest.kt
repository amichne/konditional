@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.openfeature

import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Reason
import dev.openfeature.sdk.Value
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KonditionalOpenFeatureProviderTest {
    private enum class Variant {
        CONTROL,
        TREATMENT,
    }

    private object TestFlags : Namespace.TestNamespaceFacade("openfeature-provider") {
        val enabled by boolean<Context>(default = true)
        val title by string<Context>(default = "hello")
        val retries by integer<Context>(default = 3)
        val multiplier by double<Context>(default = 1.5)
        val variant by enum<Variant, Context>(default = Variant.CONTROL)
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
    fun `string evaluation returns default with reason`() {
        val evaluation = provider.getStringEvaluation("title", "fallback", context)

        assertEquals("hello", evaluation.value)
        assertEquals(Reason.DEFAULT.name, evaluation.reason)
        assertEquals(null, evaluation.errorCode)
    }

    @Test
    fun `integer evaluation returns default with reason`() {
        val evaluation = provider.getIntegerEvaluation("retries", 0, context)

        assertEquals(3, evaluation.value)
        assertEquals(Reason.DEFAULT.name, evaluation.reason)
        assertEquals(null, evaluation.errorCode)
    }

    @Test
    fun `double evaluation returns default with reason`() {
        val evaluation = provider.getDoubleEvaluation("multiplier", 0.0, context)

        assertEquals(1.5, evaluation.value)
        assertEquals(Reason.DEFAULT.name, evaluation.reason)
        assertEquals(null, evaluation.errorCode)
    }

    @Test
    fun `object evaluation returns type mismatch when value cannot be converted to OpenFeature Value`() {
        val defaultValue = Value("fallback-object")
        val evaluation = provider.getObjectEvaluation("variant", defaultValue, context)

        assertEquals(defaultValue, evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.TYPE_MISMATCH, evaluation.errorCode)
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
    fun `missing targeting key returns invalid context via typed mapper result`() {
        val evaluation = provider.getBooleanEvaluation("enabled", false, ImmutableContext())

        assertEquals(false, evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.INVALID_CONTEXT, evaluation.errorCode)
        assertEquals(
            "OpenFeature targetingKey is required for Konditional evaluation",
            evaluation.errorMessage,
        )
    }

    @Test
    fun `blank targeting key returns invalid context via typed mapper result`() {
        val blankTargetingKeyContext = ImmutableContext(
            mapOf(EvaluationContext.TARGETING_KEY to Value("   ")),
        )
        val evaluation = provider.getBooleanEvaluation("enabled", false, blankTargetingKeyContext)

        assertEquals(false, evaluation.value)
        assertEquals(Reason.ERROR.name, evaluation.reason)
        assertEquals(ErrorCode.INVALID_CONTEXT, evaluation.errorCode)
        assertEquals(
            "OpenFeature targetingKey must not be blank for Konditional evaluation",
            evaluation.errorMessage,
        )
    }

    @Test
    fun `provider resolves known keys without repeated all flags scans`() {
        val countingRegistry = CountingNamespaceRegistry(TestFlags)
        val indexedProvider = KonditionalOpenFeatureProvider(
            namespaceRegistry = countingRegistry,
            contextMapper = TargetingKeyContextMapper(),
        )
        val defaultObject = Value("fallback-object")

        assertEquals(1, countingRegistry.allFlagsCalls)

        assertEquals(true, indexedProvider.getBooleanEvaluation("enabled", false, context).value)
        assertEquals("hello", indexedProvider.getStringEvaluation("title", "fallback", context).value)
        assertEquals(3, indexedProvider.getIntegerEvaluation("retries", 0, context).value)
        assertEquals(1.5, indexedProvider.getDoubleEvaluation("multiplier", 0.0, context).value)
        val objectEvaluation = indexedProvider.getObjectEvaluation("variant", defaultObject, context)
        assertEquals(defaultObject, objectEvaluation.value)
        assertEquals(ErrorCode.TYPE_MISMATCH, objectEvaluation.errorCode)

        assertEquals(1, countingRegistry.allFlagsCalls)
    }

    private class CountingNamespaceRegistry(
        private val delegate: NamespaceRegistry,
    ) : NamespaceRegistry by delegate {
        var allFlagsCalls: Int = 0
            private set

        override fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>> {
            allFlagsCalls += 1
            return delegate.allFlags()
        }
    }
}
