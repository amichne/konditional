package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiDocument(
    val openapi: String,
    val info: OpenApiInfo,
    val paths: Map<String, OpenApiPathItem>,
    val components: OpenApiComponents,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::openapi of { minLength = 1 }
        ::info of { description = "OpenAPI document info." }
        ::paths of { description = "Path items keyed by route template." }
        ::components of { description = "Component schema registry." }
    }
}
