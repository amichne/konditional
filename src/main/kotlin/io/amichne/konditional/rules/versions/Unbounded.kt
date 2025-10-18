package io.amichne.konditional.rules.versions

import io.amichne.konditional.context.Version

data object Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
    override fun contains(v: Version): Boolean = true
}
