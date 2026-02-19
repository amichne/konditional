file=konditional-observability/src/test/kotlin/io/amichne/konditional/ops/ShadowEvaluationTest.kt
package=io.amichne.konditional.ops
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.api.ShadowOptions,io.amichne.konditional.api.evaluateWithShadow,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,io.amichne.konditional.core.registry.InMemoryNamespaceRegistry,io.amichne.konditional.serialization.instance.Configuration,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFalse,kotlin.test.assertTrue
type=io.amichne.konditional.ops.ShadowEvaluationTest|kind=class|decl=class ShadowEvaluationTest
fields:
- private val context
methods:
- fun `evaluateWithShadow returns baseline and reports mismatched values`()
- fun `evaluateWithShadow skips candidate when baseline is disabled`()
- fun `evaluateWithShadow evaluates candidate when baseline is disabled if enabled by options`()
