@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.ops

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics

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
                enable { platforms(Platform.IOS) }
            }
        }

        assertTrue(namespace.feature.evaluate(context))

        namespace.disableAll()
        assertFalse(namespace.feature.evaluate(context))

        val diagnostics =
            namespace.feature.evaluateInternalApi(
                context = context,
                registry = namespace,
                mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
            )
        assertEquals(EvaluationDiagnostics.Decision.RegistryDisabled, diagnostics.decision)
        namespace.enableAll()
        assertTrue(namespace.feature.evaluate(context))
    }
}
