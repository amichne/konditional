@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.disable
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.core.id.TestStableId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DslSugarTest {

    private object Features : Namespace.TestNamespaceFacade("dsl-sugar") {
        val boolFlag by boolean<Context>(default = false) {
            enable { ios() }
            disable { android() }
        }

        val stringFlag by string<Context>(default = "default") {
            rule { android() } yields "android"
            rule { always() } yields "catch-all"
        }
    }

    private fun ctx(
        platform: Platform,
        locale: AppLocale = AppLocale.UNITED_STATES,
        version: Version = Version.of(1, 0, 0),
        stableId: StableId = TestStableId,
    ) = Context(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId,
    )

    @Test
    fun `enable and disable delegate to boolean rules`() {
        assertEquals(true, Features.boolFlag.evaluate(ctx(platform = Platform.IOS)))
        assertEquals(false, Features.boolFlag.evaluate(ctx(platform = Platform.ANDROID)))
    }

    @Test
    fun `rule yields declares a criteria-first rule`() {
        assertEquals("android", Features.stringFlag.evaluate(ctx(platform = Platform.ANDROID)))
        assertEquals("catch-all", Features.stringFlag.evaluate(ctx(platform = Platform.IOS)))
    }

    @Test
    fun `unclosed criteria-first rule fails fast`() {
        val error =
            assertThrows(IllegalStateException::class.java) {
                object : Namespace.TestNamespaceFacade("dsl-unclosed") {
                    val unclosed by string<Context>(default = "default") {
                        @Suppress("UnclosedCriteriaFirstRule")
                        rule { ios() }
                    }
                }
            }

        assertTrue(error.message.orEmpty().contains("yields"))
    }
}
