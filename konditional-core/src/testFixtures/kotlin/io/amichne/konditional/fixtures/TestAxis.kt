package io.amichne.konditional.fixtures

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.Axes
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.AxisValuesScope
import io.amichne.konditional.core.dsl.axis
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.core.id.StableId

/**
 * Test-only enums representing domain-specific axis values.
 */
enum class TestEnvironment(
    override val id: String,
) : AxisValue<TestEnvironment> {
    DEV("dev"),
    STAGE("stage"),
    PROD("prod"),
}

enum class TestTenant(
    override val id: String,
) : AxisValue<TestTenant> {
    CONSUMER("consumer"),
    SME("sme"),
    ENTERPRISE("enterprise")
}

/**
 * Test-only axes for the above values.
 *
 * These mirror what an application would define in its own codebase.
 */
object TestAxes {
    val Environment = Axis.of<TestEnvironment>()
    val Tenant = Axis.of<TestTenant>()
}

/**
 * Convenience helpers to make the AxisValuesScope DSL feel native.
 *
 * These would normally live in your application module.
 * Note: You can also use `axis(env, tenant)` for concise heterogeneous declarations.
 */
fun AxisValuesScope.environment(env: TestEnvironment) {
    axis(env)
}

fun AxisValuesScope.tenant(tenant: TestTenant) {
    axis(tenant)
}

/**
 * Convenient helper to declare both environment and tenant in a single call.
 */
fun AxisValuesScope.environmentAndTenant(
    env: TestEnvironment,
    tenant: TestTenant,
) {
    axis(env, tenant)
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
    override val axes: Axes = Axes.EMPTY,
) : Context,
    Context.LocaleContext,
    Context.PlatformContext,
    Context.VersionContext,
    Context.StableIdContext {
    init {
//        TestAxes.Environment
//        TestAxes.Tenant
    }
}

/**
 * Test-only feature container exercising the dimension-based DSL.
 */
object FeaturesWithAxis : Namespace.TestNamespaceFacade("dimensions-test") {
    /**
     * Enabled only when environment == PROD.
     * Uses the new axis API.
     */
    val envScopedFlag by boolean<TestContext>(default = false) {
        enable {
            constrain(TestEnvironment.PROD)
        }
    }

    /**
     * Enabled only when:
     *   environment ∈ { STAGE, PROD }
     *   AND tenant == ENTERPRISE
     * Uses the new axis API.
     */
    val envAndTenantScopedFlag by boolean<TestContext>(default = false) {
        enable {
            constrain(TestEnvironment.PROD, TestEnvironment.STAGE)
            constrain(TestTenant.ENTERPRISE)
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
        enable {
            constrain(TestEnvironment.PROD)
            constrain(TestTenant.ENTERPRISE)
        }

        enable {
            versions {
                min(2, 0, 0)
            }
        }
    }

    /**
     * Demonstrates multiple calls to constrain(...) for the same axis
     * accumulating allowed values.
     */
    val repeatedAxisFlag by boolean<TestContext>(default = false) {
        enable {
            constrain(TestEnvironment.DEV, TestEnvironment.STAGE)
        }
    }
}
