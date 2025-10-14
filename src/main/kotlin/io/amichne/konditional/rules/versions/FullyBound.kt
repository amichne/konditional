package io.amichne.konditional.rules.versions

import io.amichne.konditional.context.Version

data class FullyBound(
    override val min: Version,
    override val max: Version,
) : VersionRange(min, max, Type.FULLY_BOUND)
