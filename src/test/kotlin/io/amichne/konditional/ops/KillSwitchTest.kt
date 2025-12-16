package io.amichne.konditional.ops

import io.amichne.konditional.api.EvaluationResult.Decision.RegistryDisabled
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateWithReason
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillSwitchTest {

    private val context = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.parse("1.0.0").getOrThrow(),
        stableId = StableId.of("kill-switch-user"),
    )

    @Test
    fun `disableAll forces declared defaults`() {
        val namespace = test("kill-switch")

        val features = object : FeatureContainer<TestNamespace>(namespace) {
            val feature by boolean<Context>(default = false) {
                rule(true) { platforms(Platform.IOS) }
            }
        }

        assertTrue(features.feature.evaluate(context))

        namespace.disableAll()
        assertFalse(features.feature.evaluate(context))

        val explained = features.feature.evaluateWithReason(context)
        assertEquals(RegistryDisabled, explained.decision)
        namespace.enableAll()
        assertTrue(features.feature.evaluate(context))
    }
}
