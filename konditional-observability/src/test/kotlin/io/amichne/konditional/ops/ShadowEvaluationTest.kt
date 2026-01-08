@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.ops

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluateWithShadow
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.serialization.instance.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShadowEvaluationTest {

    private val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.parse("1.0.0").getOrThrow(),
        stableId = StableId.of("shadow-user"),
    )

    @Test
    fun `evaluateWithShadow returns baseline and reports mismatched values`() {
        val baselineNamespace = object : Namespace.TestNamespaceFacade("shadow-baseline") {
            val FLAG by boolean<Context>(default = false) {
                enable { platforms(Platform.IOS) }
            }
        }

        val candidateRegistry =
            InMemoryNamespaceRegistry(namespaceId = "shadow-candidate").apply {
                load(
                    Configuration(
                        flags = mapOf(
                            baselineNamespace.FLAG to FlagDefinition(
                                feature = baselineNamespace.FLAG,
                                bounds = emptyList(),
                                defaultValue = false,
                            ),
                        ),
                    ),
                )
            }

        var mismatched = false
        val value = baselineNamespace.FLAG.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            baselineRegistry = baselineNamespace,
            onMismatch = { mismatched = true },
        )

        assertTrue(value)
        assertTrue(mismatched)
    }

    @Test
    fun `evaluateWithShadow skips candidate when baseline is disabled`() {
        val baselineNamespace = object : Namespace.TestNamespaceFacade("shadow-disabled") {
            val FLAG by boolean<Context>(default = false) {
                enable { platforms(Platform.IOS) }
            }
        }

        val candidateRegistry =
            InMemoryNamespaceRegistry(namespaceId = "shadow-candidate").apply {
                load(
                    Configuration(
                        flags = mapOf(
                            baselineNamespace.FLAG to FlagDefinition(
                                feature = baselineNamespace.FLAG,
                                bounds = emptyList(),
                                defaultValue = true,
                            ),
                        ),
                    ),
                )
            }

        baselineNamespace.disableAll()

        var mismatched = false
        val value = baselineNamespace.FLAG.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            baselineRegistry = baselineNamespace,
            onMismatch = { mismatched = true },
        )

        assertFalse(value)
        assertEquals(false, mismatched)
    }
}
