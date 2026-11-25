package io.amichne.konditional.fixtures

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactDomain
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.context.fact.Facts
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import java.util.UUID

enum class TestEnvironment { DEV, PROD }
enum class AppLocale { UNITED_STATES, CANADA }
enum class Platform { IOS, ANDROID }

object TestFacts : FactSchema() {
    val environment = single("environment", FactDomain.enumeration(TestEnvironment.entries) { it.name })
    val platform = single("platform", FactDomain.enumeration(Platform.entries) { it.name })
    val locale = single("locale", FactDomain.enumeration(AppLocale.entries) { it.name })
}

/** A namespace over [TestFacts] with an isolated registry and identifier seed. */
abstract class TestNamespace(
    id: String,
    registry: NamespaceRegistry = InMemoryNamespaceRegistry(namespaceId = id),
    identifierSeed: String = UUID.randomUUID().toString(),
) : Namespace.TestNamespaceFacade<TestFacts>(id, TestFacts, registry, identifierSeed)

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
