package io.amichne.konditional.rules.evaluable

internal data class DimensionConstraint(
    val axisId: String,
    val allowedIds: Set<String>,
)
