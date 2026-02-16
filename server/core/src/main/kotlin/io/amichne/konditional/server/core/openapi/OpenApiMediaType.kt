package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiMediaType(
    val schema: OpenApiSchema,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::schema of { description = "Schema definition." }
    }
}
