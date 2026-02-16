@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.runtime.load
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.instance.MaterializedConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FeatureEvaluationBehaviorTest {
    private val context =
        Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.parse("1.0.0").getOrThrow(),
            stableId = StableId.of("eval-behavior-user"),
        )

    @Test
    fun `evaluate returns resolved value when feature is present`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("eval-present") {
                val feature by boolean<Context>(default = false) {
                    enable { platforms(Platform.IOS) }
                }
            }

        assertTrue(namespace.feature.evaluate(context))
    }

    @Test
    fun `evaluate throws when runtime definition is absent`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("eval-missing-definition") {
                val feature by boolean<Context>(default = false)
            }
        namespace.load(MaterializedConfiguration.of(namespace.compiledSchema(), Configuration(emptyMap())))

        val error = assertFailsWith<IllegalStateException> { namespace.feature.evaluate(context) }
        assertTrue(error.message.orEmpty().contains("Flag not found"))
    }

    @Test
    fun `evaluateInternalApi reports registry disabled decision`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("eval-disabled") {
                val feature by boolean<Context>(default = true)
            }
        namespace.disableAll()

        val diagnostics =
            namespace.feature.evaluateInternalApi(
                context = context,
                registry = namespace,
                mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
            )

        assertEquals(EvaluationDiagnostics.Decision.RegistryDisabled, diagnostics.decision)
        assertEquals(true, diagnostics.value)
    }
}
