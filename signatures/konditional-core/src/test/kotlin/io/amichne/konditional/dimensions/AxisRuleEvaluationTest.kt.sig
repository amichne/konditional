file=konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisRuleEvaluationTest.kt
package=io.amichne.konditional.dimensions
imports=io.amichne.konditional.api.axisValues,io.amichne.konditional.api.evaluate,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.unaryPlus,io.amichne.konditional.fixtures.FeaturesWithAxis,io.amichne.konditional.fixtures.TestContext,io.amichne.konditional.fixtures.TestEnvironment,io.amichne.konditional.fixtures.TestTenant,org.junit.jupiter.api.Assertions,org.junit.jupiter.api.Test
type=io.amichne.konditional.dimensions.AxisRuleEvaluationTest|kind=class|decl=class AxisRuleEvaluationTest
methods:
- private fun contextFor( env: TestEnvironment? = null, tenant: TestTenant? = null, version: String = "1.0.0", ): TestContext
- fun `ENV_SCOPED_FLAG is true only in PROD`()
- fun `ENV_AND_TENANT_SCOPED_FLAG matches env in STAGE or PROD AND tenant ENTERPRISE`()
- fun `FALLBACK_RULE_FLAG prefers more specific dimension rule over version rule`()
- fun `MULTI_CALL_DIM_FLAG accumulates values across multiple dimension calls`()
