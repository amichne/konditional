package io.amichne.konditional.server.core.openapi

import com.squareup.moshi.Json
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiParameter(
    val name: String,
    @param:Json(name = "in") val location: String,
    val required: Boolean,
    val description: String,
    val schema: OpenApiSchema,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::name of { minLength = 1 }
        ::location of { minLength = 1 }
        this@OpenApiParameter::required of { }
        ::description of { minLength = 1 }
        ::schema of { description = "OpenAPI schema node." }
    }
}
