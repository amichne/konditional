package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiComponents(
    val schemas: Map<String, OpenApiSchema>,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::schemas of { description = "Component schemas keyed by name." }
    }
}
