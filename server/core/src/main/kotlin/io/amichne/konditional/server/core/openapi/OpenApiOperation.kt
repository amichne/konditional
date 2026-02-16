package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiOperation(
    val tags: List<String>,
    val summary: String,
    val description: String,
    val operationId: String,
    val responses: Map<String, OpenApiResponse>,
    val parameters: List<OpenApiParameter>? = null,
    val requestBody: OpenApiRequestBody? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::tags of { description = "Operation tags." }
        ::summary of { minLength = 1 }
        ::description of { minLength = 1 }
        ::operationId of { minLength = 1 }
        ::responses of { description = "Responses keyed by HTTP status code." }
        ::parameters of { description = "Operation parameters." }
        ::requestBody of { description = "Optional request body." }
    }
}
