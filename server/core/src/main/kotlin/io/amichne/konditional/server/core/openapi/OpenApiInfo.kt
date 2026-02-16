package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

internal data class OpenApiInfo(
    val title: String,
    val version: String,
    val description: String,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::title of { minLength = 1 }
        ::version of { minLength = 1 }
        ::description of { minLength = 1 }
    }
}
