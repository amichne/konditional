package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.context.fact.SingleFact
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema

enum class TestEnvironment { DEV, PROD }
enum class AppLocale { UNITED_STATES, CANADA }
enum class Platform { IOS, ANDROID }

object TestFacts {
    val environment = SingleFact("environment", FactDomain.enumeration(TestEnvironment.entries) { it.name })
    val platform = SingleFact("platform", FactDomain.enumeration(Platform.entries) { it.name })
    val locale = SingleFact("locale", FactDomain.enumeration(AppLocale.entries) { it.name })
    val schema = FactSchema(environment, platform, locale)
}

data class TestContext(
    val locale: AppLocale = AppLocale.UNITED_STATES,
    val platform: Platform = Platform.IOS,
    val environment: TestEnvironment = TestEnvironment.DEV,
    override val stableId: StableId = StableId.of("fixture-user"),
) : Context.StableIdContext {
    override val facts = Facts.of(
        TestFacts.locale.value(locale),
        TestFacts.platform.value(platform),
        TestFacts.environment.value(environment),
    )
}

fun productionContext(stableId: String = "fixture-user"): TestContext =
    TestContext(stableId = StableId.of(stableId), environment = TestEnvironment.PROD)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
    val mode: String = "exponential",
) : Konstrained.Object {
    val schema =
        schema {
            ::maxAttempts of { minimum = 1 }
            ::backoffMs of { minimum = 0.0 }
            ::enabled of { default = true }
            ::mode of { minLength = 1 }
        }
}
