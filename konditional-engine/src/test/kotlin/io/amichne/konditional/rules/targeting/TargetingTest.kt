package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TargetingTest {

    // -- Locale ------------------------------------------------------------------

    @Test
    fun `Locale matches on exact locale id`() {
        val locale = Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id))
        assertTrue(locale.matches(TestContext(locale = AppLocale.UNITED_STATES)))
    }

    @Test
    fun `Locale does not match when locale absent from set`() {
        val locale = Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id))
        assertFalse(locale.matches(TestContext(locale = AppLocale.UNITED_KINGDOM)))
    }

    @Test
    fun `Locale returns false when context does not implement LocaleContext`() {
        val locale = Targeting.locale<Context>(setOf(AppLocale.UNITED_STATES.id))
        assertFalse(locale.matches(object : Context {}))
    }

    // -- Platform ----------------------------------------------------------------

    @Test
    fun `Platform matches on exact platform id`() {
        val platform = Targeting.platform<TestContext>(setOf(Platform.IOS.id))
        assertTrue(platform.matches(TestContext(platform = Platform.IOS)))
    }

    @Test
    fun `Platform does not match wrong platform`() {
        val platform = Targeting.platform<TestContext>(setOf(Platform.IOS.id))
        assertFalse(platform.matches(TestContext(platform = Platform.ANDROID)))
    }

    @Test
    fun `Platform specificity is 1`() {
        assertEquals(1, Targeting.platform<TestContext>(setOf(Platform.IOS.id)).specificity())
    }

    // -- Version -----------------------------------------------------------------

    @Test
    fun `Version matches when context version is inside FullyBound range`() {
        val range = FullyBound(Version.of(1, 0, 0), Version.of(2, 0, 0))
        val version = Targeting.version<TestContext>(range)
        assertTrue(version.matches(TestContext(appVersion = Version.of(1, 5, 0))))
        assertTrue(version.matches(TestContext(appVersion = Version.of(1, 0, 0))))
        assertTrue(version.matches(TestContext(appVersion = Version.of(2, 0, 0))))
    }

    @Test
    fun `Version does not match when context version is outside range`() {
        val range = FullyBound(Version.of(1, 0, 0), Version.of(2, 0, 0))
        val version = Targeting.version<TestContext>(range)
        assertFalse(version.matches(TestContext(appVersion = Version.of(0, 9, 9))))
        assertFalse(version.matches(TestContext(appVersion = Version.of(2, 0, 1))))
    }

    @Test
    fun `Version with Unbounded matches all versions`() {
        val version = Targeting.version<TestContext>(Unbounded)
        assertTrue(version.matches(TestContext(appVersion = Version.of(99, 99, 99))))
    }

    @Test
    fun `Version with LeftBound matches min and above`() {
        val version = Targeting.version<TestContext>(LeftBound(Version.of(2, 0, 0)))
        assertTrue(version.matches(TestContext(appVersion = Version.of(2, 0, 0))))
        assertTrue(version.matches(TestContext(appVersion = Version.of(3, 0, 0))))
        assertFalse(version.matches(TestContext(appVersion = Version.of(1, 9, 9))))
    }

    @Test
    fun `Version with RightBound matches max and below`() {
        val version = Targeting.version<TestContext>(RightBound(Version.of(2, 0, 0)))
        assertTrue(version.matches(TestContext(appVersion = Version.of(2, 0, 0))))
        assertTrue(version.matches(TestContext(appVersion = Version.of(1, 9, 9))))
        assertFalse(version.matches(TestContext(appVersion = Version.of(2, 0, 1))))
    }

    @Test
    fun `Version returns false when context does not implement VersionContext`() {
        val version = Targeting.version<Context>(FullyBound(Version.of(1, 0, 0), Version.of(2, 0, 0)))
        assertFalse(version.matches(object : Context {}))
    }

    // -- Axis --------------------------------------------------------------------

    @Test
    fun `Axis matches when context carries a matching axis value`() {
        val axis = Targeting.Axis(axisId = TestAxes.Environment.id, allowedIds = setOf(TestEnvironment.PROD.id))
        assertTrue(axis.matches(TestContext(axes = axes(TestEnvironment.PROD))))
    }

    @Test
    fun `Axis does not match when context carries a different axis value`() {
        val axis = Targeting.Axis(axisId = TestAxes.Environment.id, allowedIds = setOf(TestEnvironment.PROD.id))
        assertFalse(axis.matches(TestContext(axes = axes(TestEnvironment.DEV))))
    }

    @Test
    fun `Axis specificity is 1`() {
        assertEquals(1, Targeting.Axis(axisId = "env", allowedIds = setOf("prod")).specificity())
    }

    // -- Custom ------------------------------------------------------------------

    @Test
    fun `Custom matches when predicate returns true`() {
        val custom = Targeting.Custom<TestContext>(block = { it.platform == Platform.IOS })
        assertTrue(custom.matches(TestContext(platform = Platform.IOS)))
        assertFalse(custom.matches(TestContext(platform = Platform.ANDROID)))
    }

    @Test
    fun `Custom weight affects specificity`() {
        assertEquals(3, Targeting.Custom<TestContext>(block = { true }, weight = 3).specificity())
    }

    // -- Guarded -----------------------------------------------------------------

    @Test
    fun `Guarded matches when context satisfies evidence and inner predicate`() {
        val guarded = Targeting.whenContext<TestContext, TestContext> {
            platform == Platform.IOS
        }
        assertTrue(guarded.matches(TestContext(platform = Platform.IOS)))
        assertFalse(guarded.matches(TestContext(platform = Platform.ANDROID)))
    }

    @Test
    fun `Guarded returns false when context does not satisfy evidence cast`() {
        val guarded: Targeting<Context> = Targeting.whenContext<Context, TestContext> {
            platform == Platform.IOS
        }
        // Plain Context without platform capability - evidence returns null, guarded returns false
        assertFalse(guarded.matches(object : Context {}))
    }

    // -- AnyOf -------------------------------------------------------------------

    @Test
    fun `AnyOf matches when any branch matches`() {
        val anyOf = Targeting.AnyOf(
            listOf(
                Targeting.platform<TestContext>(setOf(Platform.IOS.id)),
                Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id)),
            ),
        )
        assertTrue(anyOf.matches(TestContext(platform = Platform.IOS, locale = AppLocale.UNITED_KINGDOM)))
        assertTrue(anyOf.matches(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_STATES)))
    }

    @Test
    fun `AnyOf does not match when no branch matches`() {
        val anyOf = Targeting.AnyOf(
            listOf(
                Targeting.platform<TestContext>(setOf(Platform.IOS.id)),
                Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id)),
            ),
        )
        assertFalse(anyOf.matches(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_KINGDOM)))
    }

    @Test
    fun `AnyOf empty list never matches`() {
        val anyOf = Targeting.AnyOf<TestContext>(emptyList())
        assertFalse(anyOf.matches(TestContext()))
    }

    @Test
    fun `AnyOf specificity is max of branches`() {
        val anyOf = Targeting.AnyOf(
            listOf(
                Targeting.Custom<TestContext>(block = { true }, weight = 2),
                Targeting.Custom<TestContext>(block = { true }, weight = 5),
            ),
        )
        assertEquals(5, anyOf.specificity())
    }

    // -- All ---------------------------------------------------------------------

    @Test
    fun `All empty list matches everything`() {
        val all = Targeting.All<TestContext>(emptyList())
        assertTrue(all.matches(TestContext()))
    }

    @Test
    fun `All matches only when all branches match`() {
        val all = Targeting.All(
            listOf(
                Targeting.platform<TestContext>(setOf(Platform.IOS.id)),
                Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id)),
            ),
        )
        assertTrue(all.matches(TestContext(platform = Platform.IOS, locale = AppLocale.UNITED_STATES)))
        assertFalse(all.matches(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_STATES)))
    }

    @Test
    fun `All specificity is sum of branch specificities`() {
        val all = Targeting.All(
            listOf(
                Targeting.platform<TestContext>(setOf(Platform.IOS.id)),
                Targeting.Custom<TestContext>(block = { true }, weight = 2),
            ),
        )
        assertEquals(3, all.specificity())
    }

    @Test
    fun `All plus combines two All instances`() {
        val left = Targeting.All(listOf(Targeting.platform<TestContext>(setOf(Platform.IOS.id))))
        val right = Targeting.All(listOf(Targeting.locale<TestContext>(setOf(AppLocale.UNITED_STATES.id))))

        val combined = left + right

        assertEquals(2, combined.targets.size)
        assertTrue(combined.matches(TestContext(platform = Platform.IOS, locale = AppLocale.UNITED_STATES)))
        assertFalse(combined.matches(TestContext(platform = Platform.ANDROID, locale = AppLocale.UNITED_STATES)))
    }

    // -- catchAll ----------------------------------------------------------------

    @Test
    fun `catchAll returns All with empty targets that matches any context`() {
        val catchAll = Targeting.catchAll<TestContext>()
        assertTrue(catchAll.matches(TestContext()))
        assertEquals(0, catchAll.specificity())
    }

    // -- VersionRange hasBounds --------------------------------------------------

    @Test
    fun `VersionRange hasBounds returns correct values`() {
        assertFalse(Unbounded.hasBounds())
        assertTrue(FullyBound(Version.of(1, 0, 0), Version.of(2, 0, 0)).hasBounds())
        assertTrue(LeftBound(Version.of(1, 0, 0)).hasBounds())
        assertTrue(RightBound(Version.of(2, 0, 0)).hasBounds())
    }
}
