package io.amichne.konditional.core.rules.versions

import io.amichne.konditional.core.context.Version

data class RightBound(
    override val max: Version,
) : VersionRange(lower, max, Type.RIGHT_BOUND)
