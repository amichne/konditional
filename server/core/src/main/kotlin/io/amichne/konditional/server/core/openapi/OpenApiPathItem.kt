package io.amichne.konditional.server.core.openapi

import io.amichne.konditional.server.core.surface.route.SurfaceHttpMethod
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiPathItem(
    val get: OpenApiOperation? = null,
    val post: OpenApiOperation? = null,
    val patch: OpenApiOperation? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::get of { description = "GET operation." }
        ::post of { description = "POST operation." }
        ::patch of { description = "PATCH operation." }
    }

    fun withOperation(
        method: SurfaceHttpMethod,
        operation: OpenApiOperation,
    ): OpenApiPathItem =
        when (method) {
            SurfaceHttpMethod.GET -> copy(get = operation)
            SurfaceHttpMethod.POST -> copy(post = operation)
            SurfaceHttpMethod.PATCH -> copy(patch = operation)
        }
}
