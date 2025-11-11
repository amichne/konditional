package io.amichne.konditional.internal.builders.versions

import io.amichne.konditional.context.Version
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

@ConsistentCopyVisibility
data class VersionRangeBuilder @PublishedApi internal constructor(
    private var leftBound: Version = Version.default,
    private var rightBound: Version = Version.default
) {

    fun min(
        major: Int,
        minor: Int = 0,
        patch: Int = 0,
    ) {
        leftBound = Version(major, minor, patch)
    }

    fun max(
        major: Int,
        minor: Int = 0,
        patch: Int = 0,
    ) {
        rightBound = Version(major, minor, patch)
    }

    fun build(): VersionRange =
        when {
            leftBound != Version.default && rightBound != Version.default -> FullyBound(leftBound, rightBound)
            leftBound == Version.default -> RightBound(rightBound)
            rightBound == Version.default -> LeftBound(leftBound)
            else -> Unbounded
        }
}
