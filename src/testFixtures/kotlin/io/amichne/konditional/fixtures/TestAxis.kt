package io.amichne.konditional.fixtures

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.dsl.RuleScope
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow

/**
 * Test-only enums representing domain-specific axis values.
 */
enum class TestEnvironment(override val id: String) : AxisValue {
    DEV("dev"),
    STAGE("stage"),
    PROD("prod"),
}

enum class TestTenant(override val id: String) : AxisValue {
    CONSUMER("consumer"),
    SME("sme"),
    ENTERPRISE("enterprise")
}

/**
 * Test-only axes for the above values.
 *
 * These mirror what an application would define in its own codebase.
 * Axes auto-register on object initialization.
 */
object TestAxes {
    object Environment : Axis<TestEnvironment>("environment", TestEnvironment::class)
    object Tenant : Axis<TestTenant>("tenant", TestTenant::class)
}

/**
 * Convenience helpers to make the AxisValuesScope DSL feel native.
 *
 * These would normally live in your application module.
 */
fun AxisValuesScope.environment(env: TestEnvironment) {
    set(TestAxes.Environment, env)
}

fun AxisValuesScope.tenant(tenant: TestTenant) {
    set(TestAxes.Tenant, tenant)
}

/**
 * Test-only context implementation wiring axis values into Konditional's Context.
 */
data class TestContext(
    override val locale: AppLocale = AppLocale.UNITED_STATES,
    override val platform: Platform = Platform.ANDROID,
    override val appVersion: Version =
        Version.parse("1.0.0").getOrThrow(),
    override val stableId: StableId = StableId.of("deadbeef"),
    override val axisValues: AxisValues = AxisValues.EMPTY,
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
object FeaturesWithAxis :
    FeatureContainer<Namespace.TestNamespaceFacade>(DimensionsTestNamespace) {

    /**
     * Enabled only when environment == PROD.
     * Uses the new axis API.
     */
    val envScopedFlag by boolean<TestContext>(default = false) {
        rule(true) {
            axis(TestAxes.Environment, TestEnvironment.PROD)
        }
    }

    /**
     * Enabled only when:
     *   environment ∈ { STAGE, PROD }
     *   AND tenant == ENTERPRISE
     * Uses the new axis API.
     */
    val envAndTenantScopedFlag by boolean<TestContext>(default = false) {
        rule(true) {
            axis(TestAxes.Environment, TestEnvironment.STAGE, TestEnvironment.PROD)
            axis(TestAxes.Tenant, TestTenant.ENTERPRISE)
        }
    }

    /**
     * Demonstrates rule specificity and fallback behaviour:
     *
     *  - Rule #1: env == PROD AND tenant == ENTERPRISE => true
     *  - Rule #2: appVersion >= 2.0.0                  => true
     *  - Otherwise: default (false)
     */
    val fallbackRuleFlag by boolean<TestContext>(default = false) {
        rule(true) {
            axis(TestAxes.Environment, TestEnvironment.PROD)
            axis(TestAxes.Tenant, TestTenant.ENTERPRISE)
        }

        rule(true) {
            versions {
                min(2, 0, 0)
            }
        }
    }

    /**
     * Demonstrates multiple calls to axis(...) for the same axis
     * accumulating allowed values.
     */
    val repeatedAxisFlag by boolean<TestContext>(default = false) {
        rule(true) {
            axis(TestAxes.Environment, TestEnvironment.DEV)
            axis(TestAxes.Environment, TestEnvironment.STAGE)
        }
    }
}

/**
 * App-specific helpers to make the rule DSL read naturally.
 *
 * These would typically live alongside your feature containers.
 * Note: With the new axis API, these helpers are less necessary since
 * you can use axis(TestEnvironment.PROD) directly.
 */
fun <C : Context> RuleScope<C>.environments(vararg envs: TestEnvironment) {
    axis(TestAxes.Environment, *envs)  // Explicit axis API
}

fun <C : Context> RuleScope<C>.tenants(vararg tenants: TestTenant) {
    axis(TestAxes.Tenant, *tenants)  // Explicit axis API
}
