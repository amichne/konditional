file=konditional-core/src/test/kotlin/io/amichne/konditional/core/MissingStableIdBucketingTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.evaluateInternalApi,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.evaluation.Bucketing,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.internal.evaluation.EvaluationDiagnostics,kotlin.test.assertEquals,kotlin.test.assertNotNull,kotlin.test.assertTrue,org.junit.jupiter.api.Assertions.assertDoesNotThrow,org.junit.jupiter.api.Test
type=io.amichne.konditional.core.MissingStableIdBucketingTest|kind=class|decl=class MissingStableIdBucketingTest
type=io.amichne.konditional.core.NoStableContext|kind=class|decl=private data class NoStableContext( override val locale: AppLocale, override val platform: Platform, override val appVersion: Version, ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext
methods:
- private fun context(): NoStableContext
- fun `missing stable id uses fallback bucket when rule is skipped by rollout`()
- fun `missing stable id still records bucket when rule matches`()
