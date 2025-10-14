package io.amichne.konditional.rules.versions

import io.amichne.konditional.context.Version

data object Unbounded : VersionRange(lower, upper, Type.UNBOUNDED) {
    override fun contains(v: Version): Boolean = true
}
