package io.amichne.konditional.fixtures

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Dimension
import io.amichne.konditional.context.DimensionKey
import io.amichne.konditional.context.Dimensions
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.DimensionScope
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow

/**
 * Test-only enums representing domain-specific dimension values.
 */
enum class TestEnvironment(override val id: String) : DimensionKey {
    DEV("dev"),
    STAGE("stage"),
    PROD("prod"),
}

enum class TestTenant(override val id: String) : DimensionKey {
    CONSUMER("consumer"),
    SME("sme"),
    ENTERPRISE("enterprise")
}

/**
 * Test-only axes for the above dimensions.
 *
 * These mirror what an application would define in its own codebase.
 */
object TestAxes {

    object Environment : Dimension<TestEnvironment> by Dimension("env")

    object Tenant : Dimension<TestTenant> by Dimension("tenant")
}

/**
 * Convenience helpers to make the ContextDimensionsBuilder DSL feel native.
 *
 * These would normally live in your application module.
 */
fun DimensionScope.environment(env: TestEnvironment) {
    set(TestAxes.Environment, env)
}

fun DimensionScope.tenant(tenant: TestTenant) {
    set(TestAxes.Tenant, tenant)
}

/**
 * Test-only context implementation wiring dimensions into Konditional's Context.
 */
data class TestContext(
    override val locale: AppLocale = AppLocale.UNITED_STATES,
    override val platform: Platform = Platform.ANDROID,
    override val appVersion: Version =
        Version.parse("1.0.0").getOrThrow(),
    override val stableId: StableId = StableId.of("deadbeef"),
    override val dimensions: Dimensions = Dimensions.EMPTY,
) : Context

/**
 * Test-only namespace to host feature flags.
 *
 * Namespace.TestNamespaceFacade is explicitly designed for tests in the library.
 */
object DimensionsTestNamespace : Namespace.TestNamespaceFacade("dimensions-test")

/**
 * Test-only feature container exercising the dimension-based DSL.
 */
object DimensionsTestFeatures :
    FeatureContainer<Namespace.TestNamespaceFacade>(DimensionsTestNamespace) {

    /**
     * Enabled only when environment == PROD.
     */
    val ENV_SCOPED_FLAG by boolean<TestContext>(default = false) {
        rule(true) {
            dimension(TestAxes.Environment, TestEnvironment.PROD)
        }
    }

    /**
     * Enabled only when:
     *   environment ∈ { STAGE, PROD }
     *   AND tenant == ENTERPRISE
     */
    val ENV_AND_TENANT_SCOPED_FLAG by boolean<TestContext>(default = false) {
        rule(true) {
            dimension(TestAxes.Environment, TestEnvironment.STAGE, TestEnvironment.PROD)
            dimension(TestAxes.Tenant, TestTenant.ENTERPRISE)
        }
    }

    /**
     * Demonstrates rule specificity and fallback behaviour:
     *
     *  - Rule #1: env == PROD AND tenant == ENTERPRISE => true
     *  - Rule #2: appVersion >= 2.0.0                  => true
     *  - Otherwise: default (false)
     */
    val FALLBACK_RULE_FLAG by boolean<TestContext>(default = false) {
        rule(true) {
            dimension(TestAxes.Environment, TestEnvironment.PROD)
            dimension(TestAxes.Tenant, TestTenant.ENTERPRISE)
        }

        rule(true) {
            versions {
                min(2, 0, 0)
            }
        }
    }

    /**
     * Demonstrates multiple calls to dimension(...) for the same axis
     * accumulating allowed values.
     */
    val MULTI_CALL_DIM_FLAG by boolean<TestContext>(default = false) {
        rule(true) {
            dimension(TestAxes.Environment, TestEnvironment.DEV)
            dimension(TestAxes.Environment, TestEnvironment.STAGE)
        }
    }
}

/**
 * App-specific helpers to make the rule DSL read naturally.
 *
 * These would typically live alongside your feature containers.
 */
fun <C : Context> RuleScope<C>.environments(vararg envs: TestEnvironment) {
    dimension(TestAxes.Environment, *envs)
}

fun <C : Context> RuleScope<C>.tenants(vararg tenants: TestTenant) {
    dimension(TestAxes.Tenant, *tenants)
}
