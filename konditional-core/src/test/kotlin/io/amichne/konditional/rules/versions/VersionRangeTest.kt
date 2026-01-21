package io.amichne.konditional.rules.versions

import io.amichne.konditional.context.Version
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for VersionRange behavior, particularly the hasBounds() method.
 */
class VersionRangeTest {

    @Test
    fun `Unbounded hasBounds returns false`() {
        assertFalse(Unbounded.hasBounds(), "Unbounded should not have bounds")
    }

    @Test
    fun `LeftBound hasBounds returns true`() {
        val leftBound = LeftBound(Version(1, 0, 0))
        assertTrue(leftBound.hasBounds(), "LeftBound should have bounds")
    }

    @Test
    fun `RightBound hasBounds returns true`() {
        val rightBound = RightBound(Version(2, 0, 0))
        assertTrue(rightBound.hasBounds(), "RightBound should have bounds")
    }

    @Test
    fun `FullyBound hasBounds returns true`() {
        val fullyBound = FullyBound(Version(1, 0, 0), Version(2, 0, 0))
        assertTrue(fullyBound.hasBounds(), "FullyBound should have bounds")
    }

    @Test
    fun `Unbounded contains all versions`() {
        assertTrue(Unbounded.contains(Version(0, 0, 0)))
        assertTrue(Unbounded.contains(Version(1, 5, 10)))
        assertTrue(Unbounded.contains(Version(999, 999, 999)))
    }

    @Test
    fun `LeftBound contains versions at or above minimum`() {
        val leftBound = LeftBound(Version(1, 0, 0))

        assertFalse(leftBound.contains(Version(0, 9, 9)))
        assertTrue(leftBound.contains(Version(1, 0, 0)))
        assertTrue(leftBound.contains(Version(1, 0, 1)))
        assertTrue(leftBound.contains(Version(2, 0, 0)))
    }

    @Test
    fun `RightBound contains versions at or below maximum`() {
        val rightBound = RightBound(Version(2, 0, 0))

        assertTrue(rightBound.contains(Version(0, 0, 0)))
        assertTrue(rightBound.contains(Version(1, 5, 0)))
        assertTrue(rightBound.contains(Version(2, 0, 0)))
        assertFalse(rightBound.contains(Version(2, 0, 1)))
    }

    @Test
    fun `FullyBound contains versions within range`() {
        val fullyBound = FullyBound(Version(1, 0, 0), Version(2, 0, 0))

        assertFalse(fullyBound.contains(Version(0, 9, 9)))
        assertTrue(fullyBound.contains(Version(1, 0, 0)))
        assertTrue(fullyBound.contains(Version(1, 5, 0)))
        assertTrue(fullyBound.contains(Version(2, 0, 0)))
        assertFalse(fullyBound.contains(Version(2, 0, 1)))
    }
}
