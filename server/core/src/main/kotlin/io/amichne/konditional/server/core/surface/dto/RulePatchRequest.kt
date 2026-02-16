package io.amichne.konditional.server.core.surface.dto

internal data class RulePatchRequest(
    val note: String? = null,
    val active: Boolean? = null,
    val rampUpPercent: Double? = null,
)
