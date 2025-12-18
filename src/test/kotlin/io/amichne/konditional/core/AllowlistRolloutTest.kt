package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.FeatureContainer
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.core.TestNamespace
import io.amichne.konditional.fixtures.core.test
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AllowlistRolloutTest {
    private fun ctx(stableId: StableId): Context =
        Context(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.parse("1.0.0").getOrThrow(),
            stableId = stableId,
        )

    @Test
    fun `rule allowlist bypasses rollout`() {
        val allowlisted = StableId.of("allowlisted-user")
        val other = StableId.of("other-user")

        val namespace = test("rule-allowlist")
        val features =
            object : FeatureContainer<TestNamespace>(namespace) {
                val feature by boolean<Context>(default = false) {
                    rule(true) {
                        rollout { 0.0 }
                        allowlist(allowlisted)
                    }
                }
            }

        assertTrue(features.feature.evaluate(ctx(allowlisted)))
        assertFalse(features.feature.evaluate(ctx(other)))
    }

    @Test
    fun `flag allowlist bypasses rollout`() {
        val allowlisted = StableId.of("allowlisted-user")
        val other = StableId.of("other-user")

        val namespace = test("flag-allowlist")
        val features =
            object : FeatureContainer<TestNamespace>(namespace) {
                val feature by boolean<Context>(default = false) {
                    allowlist(allowlisted)
                    rule(true) {
                        rollout { 0.0 }
                    }
                }
            }

        assertTrue(features.feature.evaluate(ctx(allowlisted)))
        assertFalse(features.feature.evaluate(ctx(other)))
    }
}

