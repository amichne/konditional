package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId

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

        val namespace =
            object : Namespace.TestNamespaceFacade("rule-allowlist") {
                val feature by boolean<Context>(default = false) {
                    enable {
                        rampUp { 0.0 }
                        allowlist(allowlisted)
                    }
                }
            }

        assertTrue(namespace.feature.evaluate(ctx(allowlisted)))
        assertFalse(namespace.feature.evaluate(ctx(other)))
    }

    @Test
    fun `flag allowlist bypasses rollout`() {
        val allowlisted = StableId.of("allowlisted-user")
        val other = StableId.of("other-user")

        val namespace =
            object : Namespace.TestNamespaceFacade("flag-allowlist") {
                val feature by boolean<Context>(default = false) {
                    allowlist(allowlisted)
                    enable {
                        rampUp { 0.0 }
                    }
                }
            }

        assertTrue(namespace.feature.evaluate(ctx(allowlisted)))
        assertFalse(namespace.feature.evaluate(ctx(other)))
    }
}
