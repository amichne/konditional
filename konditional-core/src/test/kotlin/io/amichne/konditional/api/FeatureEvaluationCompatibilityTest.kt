@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.registry.NamespaceRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeatureEvaluationCompatibilityTest {
    private data class CompatibilityContext(val requestId: String) : Context

    @Test
    fun `registry based evaluation uses registry flag override semantics`() {
        val namespace = object : Namespace.TestNamespaceFacade("feature-evaluation-compatibility") {
            val enabled by boolean<CompatibilityContext>(default = false)
        }

        val customRegistry = object : NamespaceRegistry by namespace {
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any, C : Context, M : Namespace> flag(
                key: Feature<T, C, M>,
            ): FlagDefinition<T, C, M> =
                if (key == namespace.enabled) {
                    namespace
                        .flag(key)
                        .copy(
                            defaultValue = true as T,
                            values = emptyList(),
                        )
                } else {
                    namespace.flag(key)
                }
        }

        val context = CompatibilityContext(requestId = "request-1")

        assertEquals(true, namespace.enabled.evaluate(context, customRegistry))
        assertEquals(true, namespace.enabled.explain(context, customRegistry).value)
    }

    @Test
    fun `evaluateWithReason remains on FeatureEvaluationKt owner`() {
        val owner = Class.forName("io.amichne.konditional.api.FeatureEvaluationKt")
        val method =
            owner.methods.singleOrNull { candidate ->
                candidate.name == "evaluateWithReason" && candidate.parameterTypes.size == 3
            }

        assertNotNull(method)
        assertEquals(Feature::class.java, method.parameterTypes[0])
        assertEquals(Context::class.java, method.parameterTypes[1])
        assertEquals(NamespaceRegistry::class.java, method.parameterTypes[2])
    }
}
