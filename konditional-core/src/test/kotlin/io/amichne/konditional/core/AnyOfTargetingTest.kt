@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.utilities.update
import kotlin.test.Test
import kotlin.test.assertEquals

class AnyOfTargetingTest {

    private object Features : Namespace.TestNamespaceFacade("anyof-targeting-test") {
        val orAcrossDimensions by boolean<Context>(default = false)
        val orAndComposition by string<Context>(default = "none")
        val specificityOrdering by string<Context>(default = "default")
    }

    private fun ctx(
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: Version = Version.of(2, 0, 0),
        idHex: String = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    ): Context = Context(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = StableId.of(idHex),
    )

    // ── OR across dimensions ─────────────────────────────────────────────

    @Test
    fun `anyOf matches when locale branch matches`() {
        Features.orAcrossDimensions.update(default = false) {
            rule(true) {
                anyOf {
                    locales(AppLocale.UNITED_STATES)
                    platforms(Platform.ANDROID)
                }
            }
        }
        // US locale matches even though platform is iOS (not Android)
        assertEquals(true, Features.orAcrossDimensions.evaluate(ctx()))
    }

    @Test
    fun `anyOf matches when platform branch matches`() {
        Features.orAcrossDimensions.update(default = false) {
            rule(true) {
                anyOf {
                    locales(AppLocale.CANADA)
                    platforms(Platform.IOS)
                }
            }
        }
        assertEquals(true, Features.orAcrossDimensions.evaluate(ctx()))
    }

    @Test
    fun `anyOf returns default when no branch matches`() {
        Features.orAcrossDimensions.update(default = false) {
            rule(true) {
                anyOf {
                    locales(AppLocale.CANADA)
                    platforms(Platform.ANDROID)
                }
            }
        }
        assertEquals(false, Features.orAcrossDimensions.evaluate(ctx()))
    }

    // ── AND + OR composition ─────────────────────────────────────────────

    @Test
    fun `anyOf combined with AND constraint requires both to match`() {
        Features.orAndComposition.update(default = "none") {
            rule("matched") {
                versions { min(1, 0, 0) }
                anyOf {
                    locales(AppLocale.CANADA)
                    platforms(Platform.IOS)
                }
            }
        }
        assertEquals("matched", Features.orAndComposition.evaluate(ctx()))
        assertEquals("none", Features.orAndComposition.evaluate(ctx(version = Version.of(0, 9, 0))))
    }

    // ── Specificity ordering ─────────────────────────────────────────────

    @Test
    fun `rule with anyOf wins over catch-all by specificity`() {
        Features.specificityOrdering.update(default = "default") {
            rule("catch-all") { always() }
            rule("or-rule") {
                anyOf {
                    locales(AppLocale.UNITED_STATES)
                    platforms(Platform.ANDROID)
                }
            }
        }
        assertEquals("or-rule", Features.specificityOrdering.evaluate(ctx()))
    }

    // ── Empty anyOf is a no-op ───────────────────────────────────────────

    @Test
    fun `empty anyOf block is silently ignored — rule remains a catch-all`() {
        Features.orAcrossDimensions.update(default = false) {
            rule(true) {
                anyOf { /* intentionally empty */ }
            }
        }
        assertEquals(true, Features.orAcrossDimensions.evaluate(ctx()))
    }
}
