file=konditional-core/src/test/kotlin/io/amichne/konditional/core/FeatureEvaluationBehaviorTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.evaluate,io.amichne.konditional.api.evaluateInternalApi,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.internal.evaluation.EvaluationDiagnostics,io.amichne.konditional.runtime.load,io.amichne.konditional.serialization.instance.Configuration,io.amichne.konditional.serialization.instance.MaterializedConfiguration,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFailsWith,kotlin.test.assertTrue
type=io.amichne.konditional.core.FeatureEvaluationBehaviorTest|kind=class|decl=class FeatureEvaluationBehaviorTest
fields:
- private val context
methods:
- fun `evaluate returns resolved value when feature is present`()
- fun `evaluate throws when runtime definition is absent`()
- fun `evaluateInternalApi reports registry disabled decision`()
