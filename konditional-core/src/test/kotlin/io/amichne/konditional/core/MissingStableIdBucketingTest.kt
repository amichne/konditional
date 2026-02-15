@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.evaluation.Bucketing
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MissingStableIdBucketingTest {
    private data class NoStableContext(
        override val locale: AppLocale,
        override val platform: Platform,
        override val appVersion: Version,
    ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext

    private fun context(): NoStableContext =
        NoStableContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.parse("1.0.0").getOrThrow(),
        )

    @Test
    fun `missing stable id uses fallback bucket when rule is skipped by rollout`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("missing-stable-id-skipped") {
                val feature by boolean<NoStableContext>(default = false) {
                    enable {
                        rampUp { 50.0 }
                    }
                }
            }

        val result =
            assertDoesNotThrow<EvaluationDiagnostics<Boolean>> {
                namespace.feature.evaluateInternalApi(
                    context = context(),
                    registry = namespace,
                    mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
                )
            }

        val decision = result.decision as EvaluationDiagnostics.Decision.Default
        val skipped = decision.skippedByRollout
        assertNotNull(skipped)
        assertEquals(Bucketing.missingStableIdBucket(), skipped.bucket.bucket)
        assertTrue(skipped.bucket.inRollout.not())
    }

    @Test
    fun `missing stable id still records bucket when rule matches`() {
        val namespace =
            object : Namespace.TestNamespaceFacade("missing-stable-id-matched") {
                val feature by boolean<NoStableContext>(default = false) {
                    enable {
                        rampUp { 100.0 }
                    }
                }
            }

        val result =
            assertDoesNotThrow<EvaluationDiagnostics<Boolean>> {
                namespace.feature.evaluateInternalApi(
                    context = context(),
                    registry = namespace,
                    mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
                )
            }

        val decision = result.decision as EvaluationDiagnostics.Decision.Rule
        assertEquals(Bucketing.missingStableIdBucket(), decision.matched.bucket.bucket)
        assertTrue(decision.matched.bucket.inRollout)
    }
}
