file=konditional-runtime/src/test/kotlin/io/amichne/konditional/ops/KillSwitchTest.kt
package=io.amichne.konditional.ops
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.api.evaluateInternalApi,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.ops.Metrics,io.amichne.konditional.internal.evaluation.EvaluationDiagnostics,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Assertions.assertFalse,org.junit.jupiter.api.Assertions.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.ops.KillSwitchTest|kind=class|decl=class KillSwitchTest
fields:
- private val context
methods:
- fun `disableAll forces declared defaults`()
