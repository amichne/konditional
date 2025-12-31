package io.amichne.konditional.dimensions

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.FeaturesWithAxis
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for axis-based rule evaluation via namespace-defined features and `Feature.evaluate`.
 */
class AxisRuleEvaluationTest {

    private fun contextFor(
        env: TestEnvironment? = null,
        tenant: TestTenant? = null,
        version: String = "1.0.0",
    ): TestContext {
        val values = axisValues {
            if (env != null) {
                +env
            }
            if (tenant != null) {
                +tenant
            }
            TestEnvironment.DEV
        }

        return TestContext(
            appVersion = Version.parse(version).getOrThrow(),
            axisValues = values,
        )
    }

    @Test
    fun `ENV_SCOPED_FLAG is true only in PROD`() {
        val prodCtx = contextFor(env = TestEnvironment.PROD)
        val stageCtx = contextFor(env = TestEnvironment.STAGE)
        val devCtx = contextFor(env = TestEnvironment.DEV)
        val noEnvCtx = contextFor(env = null)

        Assertions.assertTrue(
            FeaturesWithAxis.envScopedFlag.evaluate(prodCtx),
            "Flag should be true when environment is PROD",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.envScopedFlag.evaluate(stageCtx),
            "Flag should be false when environment is STAGE",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.envScopedFlag.evaluate(devCtx),
            "Flag should be false when environment is DEV",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.envScopedFlag.evaluate(noEnvCtx),
            "Flag should be false when environment dimension is missing",
        )
    }

    @Test
    fun `ENV_AND_TENANT_SCOPED_FLAG matches env in STAGE or PROD AND tenant ENTERPRISE`() {
        val prodEnterprise = contextFor(
            env = TestEnvironment.PROD,
            tenant = TestTenant.ENTERPRISE,
        )
        val stageEnterprise = contextFor(
            env = TestEnvironment.STAGE,
            tenant = TestTenant.ENTERPRISE,
        )
        val prodConsumer = contextFor(
            env = TestEnvironment.PROD,
            tenant = TestTenant.CONSUMER,
        )
        val devEnterprise = contextFor(
            env = TestEnvironment.DEV,
            tenant = TestTenant.ENTERPRISE,
        )

        Assertions.assertTrue(
            FeaturesWithAxis.envAndTenantScopedFlag.evaluate(prodEnterprise),
            "Flag should be true for PROD + ENTERPRISE",
        )
        Assertions.assertTrue(
            FeaturesWithAxis.envAndTenantScopedFlag.evaluate(stageEnterprise),
            "Flag should be true for STAGE + ENTERPRISE",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.envAndTenantScopedFlag.evaluate(prodConsumer),
            "Flag should be false when tenant is not ENTERPRISE",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.envAndTenantScopedFlag.evaluate(devEnterprise),
            "Flag should be false when environment is not STAGE or PROD",
        )
    }

    @Test
    fun `FALLBACK_RULE_FLAG prefers more specific dimension rule over version rule`() {
        val prodEnterpriseV1 = contextFor(
            env = TestEnvironment.PROD,
            tenant = TestTenant.ENTERPRISE,
            version = "1.0.0",
        )

        val prodConsumerV3 = contextFor(
            env = TestEnvironment.PROD,
            tenant = TestTenant.CONSUMER,
            version = "3.0.0",
        )

        val devconsumerv15 = contextFor(
            env = TestEnvironment.DEV,
            tenant = TestTenant.CONSUMER,
            version = "1.5.0",
        )

        // Rule #1 should match regardless create version
        Assertions.assertTrue(
            FeaturesWithAxis.fallbackRuleFlag.evaluate(prodEnterpriseV1),
            "Specific env+tenant rule should match first",
        )

        // Rule #1 fails (tenant != ENTERPRISE); Rule #2 should match by version
        Assertions.assertTrue(
            FeaturesWithAxis.fallbackRuleFlag.evaluate(prodConsumerV3),
            "Fallback version-based rule should match when version >= 2.0.0",
        )

        // Neither rule matches; default should be returned (false)
        Assertions.assertFalse(
            FeaturesWithAxis.fallbackRuleFlag.evaluate(devconsumerv15),
            "No rules should match for DEV + version < 2.0.0",
        )
    }

    @Test
    fun `MULTI_CALL_DIM_FLAG accumulates values across multiple dimension calls`() {
        val devCtx = contextFor(env = TestEnvironment.DEV)
        val stageCtx = contextFor(env = TestEnvironment.STAGE)
        val prodCtx = contextFor(env = TestEnvironment.PROD)

        Assertions.assertTrue(
            FeaturesWithAxis.repeatedAxisFlag.evaluate(devCtx),
            "Flag should be true when env == DEV",
        )
        Assertions.assertTrue(
            FeaturesWithAxis.repeatedAxisFlag.evaluate(stageCtx),
            "Flag should be true when env == STAGE",
        )
        Assertions.assertFalse(
            FeaturesWithAxis.repeatedAxisFlag.evaluate(prodCtx),
            "Flag should be false when env == PROD",
        )
    }
}
