package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiResponse(
    val description: String,
    val content: OpenApiContent? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::description of { minLength = 1 }
        ::content of { description = "Optional response content." }
    }
}
