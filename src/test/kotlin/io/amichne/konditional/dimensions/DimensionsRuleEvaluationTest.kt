package io.amichne.konditional.dimensions

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.evaluate
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fix.dimensions
import io.amichne.konditional.fixtures.DimensionsTestFeatures
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.environment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Unit tests for dimension-based rule evaluation via FeatureContainer & Feature.evaluate.
 */
class DimensionsRuleEvaluationTest {

    private fun contextFor(
        env: TestEnvironment? = null,
        tenant: TestTenant? = null,
        version: String = "1.0.0",
    ): TestContext {
        val dims = dimensions {
            if (env != null) environment(env)
            if (tenant != null) {
                this[TestAxes.Tenant] = tenant
            }
        }

        return TestContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.ANDROID,
            appVersion = Version.parse(version).getOrThrow(),
            stableId = StableId.of("deadbeef"),
            dimensions = dims,
        )
    }

    @Test
    fun `ENV_SCOPED_FLAG is true only in PROD`() {
        val prodCtx = contextFor(env = TestEnvironment.PROD)
        val stageCtx = contextFor(env = TestEnvironment.STAGE)
        val devCtx = contextFor(env = TestEnvironment.DEV)
        val noEnvCtx = contextFor(env = null)

        Assertions.assertTrue(
            DimensionsTestFeatures.ENV_SCOPED_FLAG.evaluate(prodCtx),
            "Flag should be true when environment is PROD",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.ENV_SCOPED_FLAG.evaluate(stageCtx),
            "Flag should be false when environment is STAGE",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.ENV_SCOPED_FLAG.evaluate(devCtx),
            "Flag should be false when environment is DEV",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.ENV_SCOPED_FLAG.evaluate(noEnvCtx),
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
            DimensionsTestFeatures.ENV_AND_TENANT_SCOPED_FLAG.evaluate(prodEnterprise),
            "Flag should be true for PROD + ENTERPRISE",
        )
        Assertions.assertTrue(
            DimensionsTestFeatures.ENV_AND_TENANT_SCOPED_FLAG.evaluate(stageEnterprise),
            "Flag should be true for STAGE + ENTERPRISE",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.ENV_AND_TENANT_SCOPED_FLAG.evaluate(prodConsumer),
            "Flag should be false when tenant is not ENTERPRISE",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.ENV_AND_TENANT_SCOPED_FLAG.evaluate(devEnterprise),
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

        // Rule #1 should match regardless of version
        Assertions.assertTrue(
            DimensionsTestFeatures.FALLBACK_RULE_FLAG.evaluate(prodEnterpriseV1),
            "Specific env+tenant rule should match first",
        )

        // Rule #1 fails (tenant != ENTERPRISE); Rule #2 should match by version
        Assertions.assertTrue(
            DimensionsTestFeatures.FALLBACK_RULE_FLAG.evaluate(prodConsumerV3),
            "Fallback version-based rule should match when version >= 2.0.0",
        )

        // Neither rule matches; default should be returned (false)
        Assertions.assertFalse(
            DimensionsTestFeatures.FALLBACK_RULE_FLAG.evaluate(devconsumerv15),
            "No rules should match for DEV + version < 2.0.0",
        )
    }

    @Test
    fun `MULTI_CALL_DIM_FLAG accumulates values across multiple dimension calls`() {
        val devCtx = contextFor(env = TestEnvironment.DEV)
        val stageCtx = contextFor(env = TestEnvironment.STAGE)
        val prodCtx = contextFor(env = TestEnvironment.PROD)

        Assertions.assertTrue(
            DimensionsTestFeatures.MULTI_CALL_DIM_FLAG.evaluate(devCtx),
            "Flag should be true when env == DEV",
        )
        Assertions.assertTrue(
            DimensionsTestFeatures.MULTI_CALL_DIM_FLAG.evaluate(stageCtx),
            "Flag should be true when env == STAGE",
        )
        Assertions.assertFalse(
            DimensionsTestFeatures.MULTI_CALL_DIM_FLAG.evaluate(prodCtx),
            "Flag should be false when env == PROD",
        )
    }
}
