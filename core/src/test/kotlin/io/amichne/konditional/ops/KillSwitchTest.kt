package io.amichne.konditional.ops

import io.amichne.konditional.api.EvaluationResult.Decision.RegistryDisabled
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
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
        val namespace = object : Namespace.TestNamespaceFacade("kill-switch") {
            val feature by boolean<Context>(default = false) {
                rule(true) { platforms(Platform.IOS) }
            }
        }

        assertTrue(namespace.feature.evaluate(context))

        namespace.disableAll()
        assertFalse(namespace.feature.evaluate(context))

        val explained = namespace.feature.explain(context)
        assertEquals(RegistryDisabled, explained.decision)
        namespace.enableAll()
        assertTrue(namespace.feature.evaluate(context))
    }
}
