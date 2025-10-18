package io.amichne.konditional.rules.versions

import io.amichne.konditional.context.Version

sealed class VersionRange protected constructor(
    val type: Type,
    open val min: Version? = null,
    open val max: Version? = null,
) {
    enum class Type {
        LEFT_BOUND,
        RIGHT_BOUND,
        FULLY_BOUND,
        UNBOUNDED,
    }

    companion object {
        internal val MIN_VERSION = Version(0, 0, 0)
        internal val MAX_VERSION = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
    }

    open fun contains(v: Version): Boolean = (min?.let { v >= it } ?: true) && (max?.let { v <= it } ?: true)

    fun hasBounds(): Boolean = (min != null) || (max != null)
}
