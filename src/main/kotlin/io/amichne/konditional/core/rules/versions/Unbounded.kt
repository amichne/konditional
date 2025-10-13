package io.amichne.konditional.core.rules.versions

import io.amichne.konditional.core.context.Version

data object Unbounded : VersionRange(lower, upper, Type.UNBOUNDED) {
    override fun contains(v: Version): Boolean = true
}
