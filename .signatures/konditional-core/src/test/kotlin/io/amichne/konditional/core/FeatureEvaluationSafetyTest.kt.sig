file=konditional-core/src/test/kotlin/io/amichne/konditional/core/FeatureEvaluationSafetyTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.api.evaluateSafely,io.amichne.konditional.api.explainSafely,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFailsWith,kotlin.test.assertIs,kotlin.test.assertTrue
type=io.amichne.konditional.core.FeatureEvaluationSafetyTest|kind=class|decl=class FeatureEvaluationSafetyTest
fields:
- private val context
methods:
- fun `evaluateSafely returns success when feature exists`()
- fun `evaluateSafely returns typed feature-not-found error when definition is absent`()
- fun `explainSafely returns typed feature-not-found error when definition is absent`()
- fun `evaluate keeps throwing behavior for missing features`()
