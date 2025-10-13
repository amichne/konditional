package io.amichne.konditional.core.rules.versions

import io.amichne.konditional.core.context.Version

data class FullyBound(
    override val min: Version,
    override val max: Version,
) : VersionRange(min, max, Type.FULLY_BOUND)
