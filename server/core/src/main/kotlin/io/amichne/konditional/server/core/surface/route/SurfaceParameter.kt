package io.amichne.konditional.server.core.surface.route

import io.amichne.kontracts.schema.JsonSchema

internal data class SurfaceParameter(
    val name: String,
    val location: SurfaceParameterLocation,
    val required: Boolean,
    val description: String,
    val schema: JsonSchema<*>,
)
