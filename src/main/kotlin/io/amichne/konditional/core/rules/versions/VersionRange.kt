package io.amichne.konditional.core.rules.versions

import io.amichne.konditional.core.context.Version

abstract class VersionRange protected constructor(
    open val min: Version? = null,
    open val max: Version? = null,
    val type: Type = Type.LEGACY
) {
    enum class Type {
        LEFT_BOUND,
        RIGHT_BOUND,
        FULLY_BOUND,
        UNBOUNDED,

        @Deprecated("tmp")
        LEGACY
    }

    companion object {
        /**
         * Lower bound for version ranges.
         */
        val lower = Version(0, 0, 0)

        /**
         * Upper bound for version ranges.
         */
        val upper = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        @Deprecated("Use the explicitly typed VersionRange")
        operator fun invoke(min: Version? = null, max: Version? = null): VersionRange =
            object : VersionRange(min, max) {}
    }

    open fun contains(v: Version): Boolean =
        (min?.let { v >= it } ?: true) && (max?.let { v <= it } ?: true)

    fun hasBounds(): Boolean = (min != null) || (max != null)
}
