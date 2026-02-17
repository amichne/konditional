package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.fixtures.EnterpriseContext
import io.amichne.konditional.fixtures.SubscriptionTier
import io.amichne.konditional.fixtures.UserRole
import io.amichne.konditional.fixtures.core.id.TestStableId
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the [Targeting] sealed hierarchy covering leaf correctness,
 * [Targeting.Guarded] non-matching semantics, [Targeting.All] combinator behavior,
 * multi-dimensional targeting, and capability narrowing via `whenContext`.
 */
class TargetingHierarchyTest {

    // -- Test contexts --

    private val usIosContext = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version(2, 0, 0),
        stableId = TestStableId,
    )

    private val caAndroidContext = Context(
        locale = AppLocale.CANADA,
        platform = Platform.ANDROID,
        appVersion = Version(1, 0, 0),
        stableId = TestStableId,
    )

    private val minimalContext = object : Context {}

    private val premiumIosUsContext = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version(2, 0, 0),
        stableId = TestStableId,
        organizationId = "org-1",
        subscriptionTier = SubscriptionTier.PREMIUM,
        userRole = UserRole.ADMIN,
    )

    private val basicAndroidUsContext = EnterpriseContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.ANDROID,
        appVersion = Version(2, 0, 0),
        stableId = TestStableId,
        organizationId = "org-2",
        subscriptionTier = SubscriptionTier.BASIC,
        userRole = UserRole.EDITOR,
    )

    // ── 1. Leaf correctness ─────────────────────────────────────────────

    @Test
    fun `Locale leaf matches context with matching locale id`() {
        val locale = Targeting.Locale(setOf(AppLocale.UNITED_STATES.id))
        assertTrue(locale.matches(usIosContext))
    }

    @Test
    fun `Locale leaf rejects context with non-matching locale id`() {
        val locale = Targeting.Locale(setOf(AppLocale.CANADA.id))
        assertFalse(locale.matches(usIosContext))
    }

    @Test
    fun `Platform leaf matches context with matching platform`() {
        val platform = Targeting.Platform(setOf(Platform.IOS.id))
        assertTrue(platform.matches(usIosContext))
    }

    @Test
    fun `Platform leaf returns false for non-matching platform`() {
        val platform = Targeting.Platform(setOf(Platform.ANDROID.id))
        assertFalse(platform.matches(usIosContext))
    }

    @Test
    fun `Version leaf matches context within range`() {
        val version = Targeting.Version(LeftBound(Version(1, 0, 0)))
        assertTrue(version.matches(usIosContext)) // 2.0.0 >= 1.0.0
    }

    @Test
    fun `Version leaf rejects context outside range`() {
        val version = Targeting.Version(LeftBound(Version(3, 0, 0)))
        assertFalse(version.matches(usIosContext)) // 2.0.0 < 3.0.0
    }

    @Test
    fun `Version leaf with Unbounded contributes zero specificity`() {
        assertEquals(0, Targeting.Version(Unbounded).specificity())
    }

    @Test
    fun `Version leaf with bound contributes one specificity`() {
        assertEquals(1, Targeting.Version(LeftBound(Version(1, 0, 0))).specificity())
    }

    @Test
    fun `Custom leaf evaluates block correctly`() {
        val custom = Targeting.Custom<Context.LocaleContext>(
            block = { it.locale.id == AppLocale.UNITED_STATES.id },
            weight = 2,
        )
        assertTrue(custom.matches(usIosContext))
        assertEquals(2, custom.specificity())
    }

    // ── 2. Guarded non-matching semantics ───────────────────────────────

    @Test
    fun `Guarded returns false when context lacks capability`() {
        val target = Targeting.locale<Context>(setOf(AppLocale.UNITED_STATES.id))
        assertFalse(target.matches(minimalContext))
    }

    @Test
    fun `Guarded returns true when context has capability and matches`() {
        val target = Targeting.locale<Context>(setOf(AppLocale.UNITED_STATES.id))
        assertTrue(target.matches(usIosContext))
    }

    @Test
    fun `Guarded platform returns false when context lacks PlatformContext`() {
        val target = Targeting.platform<Context>(setOf(Platform.IOS.id))
        assertFalse(target.matches(minimalContext))
    }

    @Test
    fun `Guarded version returns false when context lacks VersionContext`() {
        val target = Targeting.version<Context>(LeftBound(Version(1, 0, 0)))
        assertFalse(target.matches(minimalContext))
    }

    // ── 3. All combinator ───────────────────────────────────────────────

    @Test
    fun `All with empty targets matches everything`() {
        assertTrue(Targeting.catchAll<Context>().matches(minimalContext))
    }

    @Test
    fun `All specificity is sum of leaves`() {
        val all = Targeting.All(
            listOf(
                Targeting.locale<Context>(setOf(AppLocale.UNITED_STATES.id)),
                Targeting.platform<Context>(setOf(Platform.IOS.id)),
                Targeting.Custom<Context>({ true }, weight = 2),
            ),
        )
        assertEquals(4, all.specificity())
    }

    @Test
    fun `All short-circuits on first non-matching leaf`() {
        var evaluated = 0
        val all = Targeting.All(
            listOf(
                Targeting.Custom<Context>({ false }),
                Targeting.Custom<Context>({ evaluated++; true }),
            ),
        )
        assertFalse(all.matches(minimalContext))
        assertEquals(0, evaluated) // second leaf never evaluated
    }

    @Test
    fun `All plus combines two conjunctions`() {
        val a = Targeting.All(listOf(Targeting.locale<Context>(setOf("en-US"))))
        val b = Targeting.All(listOf(Targeting.platform<Context>(setOf("iOS"))))
        val combined = a + b
        assertEquals(2, combined.targets.size)
        assertEquals(2, combined.specificity())
    }

    @Test
    fun `catchAll has zero specificity`() {
        assertEquals(0, Targeting.catchAll<Context>().specificity())
    }

    // ── 4. Multi-dimensional EnterpriseContext ───────────────────────────

    @Test
    fun `multi-dimensional rule matches premium iOS US user`() {
        val rule = Rule<EnterpriseContext>(
            targeting = Targeting.All(
                listOf(
                    Targeting.locale(setOf(AppLocale.UNITED_STATES.id)),
                    Targeting.platform(setOf(Platform.IOS.id)),
                    Targeting.Custom<EnterpriseContext>(block = { it.subscriptionTier == SubscriptionTier.PREMIUM }),
                ),
            ),
        )
        assertTrue(rule.matches(premiumIosUsContext))
        assertFalse(rule.matches(basicAndroidUsContext))
    }

    @Test
    fun `multi-dimensional rule specificity is 3`() {
        val rule = Rule<EnterpriseContext>(
            targeting = Targeting.All(
                listOf(
                    Targeting.locale(setOf(AppLocale.UNITED_STATES.id)),
                    Targeting.platform(setOf(Platform.IOS.id)),
                    Targeting.Custom<EnterpriseContext>(block = { it.subscriptionTier == SubscriptionTier.PREMIUM }),
                ),
            ),
        )
        assertEquals(3, rule.specificity())
    }

    // ── 5. whenContext capability narrowing ──────────────────────────────

    @Test
    fun `whenContext returns false for context lacking the capability`() {
        val targeting = Targeting.whenContext<Context, EnterpriseContext> {
            subscriptionTier == SubscriptionTier.PREMIUM
        }
        // A bare Context has no EnterpriseContext mixin -> false, no throw
        assertFalse(targeting.matches(usIosContext))
    }

    @Test
    fun `whenContext fires correctly when context implements R`() {
        val targeting = Targeting.whenContext<Context, EnterpriseContext> {
            subscriptionTier == SubscriptionTier.PREMIUM
        }
        assertTrue(targeting.matches(premiumIosUsContext))
        assertFalse(targeting.matches(basicAndroidUsContext))
    }

    @Test
    fun `whenContext contributes correct specificity`() {
        val targeting = Targeting.whenContext<Context, EnterpriseContext>(weight = 3) {
            subscriptionTier == SubscriptionTier.PREMIUM
        }
        assertEquals(3, targeting.specificity())
    }

    // ── 6. Determinism ──────────────────────────────────────────────────

    @Test
    fun `same inputs produce same result across repeated evaluations`() {
        val targeting = Targeting.All<Context>(
            listOf(
                Targeting.locale(setOf(AppLocale.UNITED_STATES.id)),
                Targeting.platform(setOf(Platform.IOS.id)),
                Targeting.version(LeftBound(Version(1, 0, 0))),
            ),
        )
        repeat(100) {
            assertTrue(targeting.matches(usIosContext))
            assertFalse(targeting.matches(caAndroidContext))
        }
    }

    // ── 7. Projection helpers ───────────────────────────────────────────

    @Test
    fun `localesOrEmpty extracts locale ids from Guarded-Locale leaves`() {
        val all = Targeting.All<Context>(
            listOf(
                Targeting.locale(setOf("en-US", "en-CA")),
                Targeting.platform(setOf("iOS")),
            ),
        )
        assertEquals(setOf("en-US", "en-CA"), all.localesOrEmpty())
    }

    @Test
    fun `platformsOrEmpty extracts platform ids from Guarded-Platform leaves`() {
        val all = Targeting.All<Context>(
            listOf(
                Targeting.locale(setOf("en-US")),
                Targeting.platform(setOf("iOS", "Android")),
            ),
        )
        assertEquals(setOf("iOS", "Android"), all.platformsOrEmpty())
    }

    @Test
    fun `versionRangeOrNull extracts version range from Guarded-Version leaf`() {
        val range = LeftBound(Version(1, 0, 0))
        val all = Targeting.All<Context>(
            listOf(Targeting.version(range)),
        )
        assertEquals(range, all.versionRangeOrNull())
    }

    @Test
    fun `versionRangeOrNull returns null when no version leaf present`() {
        val all = Targeting.All<Context>(
            listOf(Targeting.locale(setOf("en-US"))),
        )
        assertEquals(null, all.versionRangeOrNull())
    }

    @Test
    fun `axesOrEmpty extracts axis constraints`() {
        val all = Targeting.All<Context>(
            listOf(
                Targeting.Axis("env", setOf("prod", "stage")),
                Targeting.Axis("region", setOf("us-east")),
            ),
        )
        assertEquals(
            mapOf("env" to setOf("prod", "stage"), "region" to setOf("us-east")),
            all.axesOrEmpty(),
        )
    }

    @Test
    fun `customLeafCount counts Custom and Guarded-Custom leaves`() {
        val all = Targeting.All<Context>(
            listOf(
                Targeting.locale(setOf("en-US")),       // Guarded(Locale) -> not custom
                Targeting.Custom<Context>({ true }),     // direct Custom
                Targeting.whenContext<Context, EnterpriseContext> { true }, // Guarded(Custom)
            ),
        )
        assertEquals(2, all.customLeafCount())
    }
}
