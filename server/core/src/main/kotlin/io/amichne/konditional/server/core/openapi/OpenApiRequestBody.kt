package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiRequestBody(
    val description: String,
    val required: Boolean,
    val content: OpenApiContent,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::description of { minLength = 1 }
        this@OpenApiRequestBody::required of { }
        ::content of { description = "Request body content by media type." }
    }
}
