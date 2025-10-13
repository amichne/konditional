package io.amichne.konditional.core.rules.versions

import io.amichne.konditional.core.context.Version

data class LeftBound(
    override val min: Version,
) : VersionRange(min, upper, Type.LEFT_BOUND)
