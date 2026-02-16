package io.amichne.konditional.server.core.openapi

import com.squareup.moshi.Json
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiContent(
    @param:Json(name = "application/json") val applicationJson: OpenApiMediaType,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::applicationJson of { description = "application/json payload." }
    }
}
