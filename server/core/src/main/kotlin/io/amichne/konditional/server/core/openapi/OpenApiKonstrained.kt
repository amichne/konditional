package io.amichne.konditional.server.core.openapi

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectTraits

internal interface OpenApiKonstrained<out S> where S : JsonSchema<*>, S : ObjectTraits {
    val contractSchema: S
}
