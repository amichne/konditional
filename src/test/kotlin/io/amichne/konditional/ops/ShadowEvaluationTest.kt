package io.amichne.konditional.ops

import io.amichne.konditional.api.evaluateWithShadow
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.result.getOrThrow
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
        val baselineNamespace = Namespace("shadow-baseline")

        val features = object : FeatureContainer<Namespace>(baselineNamespace) {
            val FLAG by boolean<Context>(default = false) {
                rule(true) { platforms(Platform.IOS) }
            }
        }

        val candidateRegistry = NamespaceRegistry(
            namespaceId = "shadow-candidate",
            configuration = Configuration(
                flags = mapOf(
                    features.FLAG to FlagDefinition(
                        feature = features.FLAG,
                        bounds = emptyList(),
                        defaultValue = false,
                    )
                )
            )
        )

        var mismatched = false
        val value = features.FLAG.evaluateWithShadow(
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
        val baselineNamespace = Namespace("shadow-disabled")

        val features = object : FeatureContainer<Namespace>(baselineNamespace) {
            val FLAG by boolean<Context>(default = false) {
                rule(true) { platforms(Platform.IOS) }
            }
        }

        val candidateRegistry: NamespaceRegistry = NamespaceRegistry(
            namespaceId = "shadow-candidate",
            configuration = Configuration(
                flags = mapOf(
                    features.FLAG to FlagDefinition(
                        feature = features.FLAG,
                        bounds = emptyList(),
                        defaultValue = true,
                    )
                )
            )
        )

        baselineNamespace.disableAll()

        var mismatched = false
        val value = features.FLAG.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            baselineRegistry = baselineNamespace,
            onMismatch = { mismatched = true },
        )

        assertFalse(value)
        assertEquals(false, mismatched)
    }
}
