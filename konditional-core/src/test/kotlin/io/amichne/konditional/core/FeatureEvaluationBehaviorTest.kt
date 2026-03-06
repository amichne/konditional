@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.explain
import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.runtime.update
import io.amichne.konditional.serialization.instance.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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
        namespace.update(Configuration(emptyMap()))

        val error = assertFailsWith<IllegalStateException> { namespace.feature.evaluate(context) }
        assertTrue(error.message.orEmpty().contains("Flag not found"))
    }

    @Test
    fun `explain returns deterministic diagnostics for same input`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("eval-explain") {
                val feature by boolean<Context>(default = false) {
                    enable { platforms(Platform.IOS) }
                }
            }

        val first = namespace.feature.explain(context)
        val second = namespace.feature.explain(context)

        assertEquals(Metrics.Evaluation.EvaluationMode.EXPLAIN, first.mode)
        assertEquals(true, first.value)
        assertEquals(first.copy(durationNanos = 0L), second.copy(durationNanos = 0L))
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

    @Test
    fun `evaluateInternalApi emits rule identity and extension context nodes`() {
        data class EnterpriseContext(
            override val locale: AppLocale,
            override val platform: Platform,
            override val appVersion: Version,
            val tenant: String,
            val isEmployee: Boolean,
            override val stableId: StableId,
        ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext

        val namespace =
            object : Namespace.TestNamespaceFacade("eval-rule-id") {
                val feature by string<EnterpriseContext>(default = "default") {
                    rule("enterprise") {
                        whenContext<Context.PlatformContext> { platform == Platform.IOS }
                        extension { tenant == "acme" }
                    }
                    rule("employee") {
                        extension { isEmployee }
                    }
                }
            }

        val diagnostics =
            namespace.feature.evaluateInternalApi(
                context =
                    EnterpriseContext(
                        locale = AppLocale.UNITED_STATES,
                        platform = Platform.IOS,
                        appVersion = Version.of(1, 0, 0),
                        tenant = "acme",
                        isEmployee = true,
                        stableId = StableId.of("eval-rule-id-user"),
                    ),
                registry = namespace,
                mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
            )

        val decision = diagnostics.decision as EvaluationDiagnostics.Decision.Rule
        val matched = decision.matched

        assertTrue(matched.ruleId.startsWith("rule::${namespace.id.value}::${namespace.feature.key}::"))
        assertEquals(EvaluationDiagnostics.ExtensionType.LAMBDA, matched.extensionNode.type)
        assertEquals(EvaluationDiagnostics.ConditionalContextType.NARROWING, matched.conditionalContextNode.type)
        assertNotEquals(
            EvaluationDiagnostics.ExtensionNode(EvaluationDiagnostics.ExtensionType.NONE),
            matched.extensionNode,
        )
    }

    @Test
    fun `explain exposes generic rule match details without extra navigation`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("eval-direct-rule-match") {
                val feature by string<Context>(default = "default") {
                    rule("ios") {
                        platforms(Platform.IOS)
                    }
                }
            }

        val diagnostics = namespace.feature.explain(context)
        val decision = diagnostics.decision as EvaluationDiagnostics.Decision.Rule
        val summary = summarize(decision.matched)

        assertEquals("ios", diagnostics.value)
        assertEquals("rule::${namespace.id.value}::${namespace.feature.key}", summary.ruleIdPrefix)
        assertEquals(1, summary.specificity)
        assertEquals(null, summary.note)
    }

    private fun <D : EvaluationDiagnostics.RuleDetails> summarize(
        match: EvaluationDiagnostics.RuleMatch<D>,
    ): RuleMatchSummary =
        RuleMatchSummary(
            ruleIdPrefix = match.ruleId.substringBeforeLast("::"),
            specificity = match.totalSpecificity,
            note = match.note,
        )

    private data class RuleMatchSummary(
        val ruleIdPrefix: String,
        val specificity: Int,
        val note: String?,
    )
}
