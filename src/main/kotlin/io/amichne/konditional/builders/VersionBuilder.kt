package io.amichne.konditional.builders

import io.amichne.konditional.core.FeatureFlagDsl
import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.versions.VersionRange

@FeatureFlagDsl
class VersionBuilder {
    private var minVersion: Version? = null
    private var maxVersion: Version? = null

    /**
     * Sets the minimum version constraint to at least the specified major version.
     * e.g., atLeast(2) means >= 2.0.0
     */
    fun atLeast(major: Int) {
        minVersion = Version(major, 0, 0)
    }

    /**
     * Sets the minimum version constraint to at least the specified major.minor version.
     * e.g., atLeast(2, 3) means >= 2.3.0
     */
    fun atLeast(major: Int, minor: Int) {
        minVersion = Version(major, minor, 0)
    }

    /**
     * Sets the minimum version constraint to at least the specified major.minor.patch version.
     * e.g., atLeast(2, 3, 4) means >= 2.3.4
     */
    fun atLeast(major: Int, minor: Int, patch: Int) {
        minVersion = Version(major, minor, patch)
    }

    /**
     * Sets the maximum version constraint to at most the specified major version.
     * e.g., atMost(3) means <= 3.0.0
     */
    fun atMost(major: Int) {
        maxVersion = Version(major, 0, 0)
    }

    /**
     * Sets the maximum version constraint to at most the specified major.minor version.
     * e.g., atMost(2, 4) means <= 2.4.0
     */
    fun atMost(major: Int, minor: Int) {
        maxVersion = Version(major, minor, 0)
    }

    /**
     * Sets the maximum version constraint to at most the specified major.minor.patch version.
     * e.g., atMost(2, 3, 5) means <= 2.3.5
     */
    fun atMost(major: Int, minor: Int, patch: Int) {
        maxVersion = Version(major, minor, patch)
    }

    @Suppress("DEPRECATION")
    fun build(): VersionRange = VersionRange(minVersion, maxVersion)
}
