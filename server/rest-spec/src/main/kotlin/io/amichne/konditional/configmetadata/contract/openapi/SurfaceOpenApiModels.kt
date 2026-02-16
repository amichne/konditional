package io.amichne.konditional.configmetadata.contract.openapi

import com.squareup.moshi.Json
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.schema.ObjectTraits

internal interface OpenApiKonstrained<out S> where S : JsonSchema<*>, S : ObjectTraits {
    val contractSchema: S
}

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

internal data class OpenApiComponents(
    val schemas: Map<String, JsonSchema<*>>,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::schemas of { description = "Component schemas keyed by name." }
    }
}

internal data class OpenApiPathItem(
    val get: OpenApiOperation? = null,
    val post: OpenApiOperation? = null,
    val patch: OpenApiOperation? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::get of { description = "GET operation." }
        ::post of { description = "POST operation." }
        ::patch of { description = "PATCH operation." }
    }

    fun withOperation(
        method: SurfaceHttpMethod,
        operation: OpenApiOperation,
    ): OpenApiPathItem =
        when (method) {
            SurfaceHttpMethod.GET -> copy(get = operation)
            SurfaceHttpMethod.POST -> copy(post = operation)
            SurfaceHttpMethod.PATCH -> copy(patch = operation)
        }
}

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

internal data class OpenApiParameter(
    val name: String,
    @param:Json(name = "in") val location: String,
    val required: Boolean,
    val description: String,
    val schema: JsonSchema<*>,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::name of { minLength = 1 }
        ::location of { minLength = 1 }
        this@OpenApiParameter::required of { }
        ::description of { minLength = 1 }
        ::schema of { description = "OpenAPI schema node." }
    }
}

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

internal data class OpenApiResponse(
    val description: String,
    val content: OpenApiContent? = null,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::description of { minLength = 1 }
        ::content of { description = "Optional response content." }
    }
}

internal data class OpenApiContent(
    @param:Json(name = "application/json") val applicationJson: OpenApiMediaType,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::applicationJson of { description = "application/json payload." }
    }
}

internal data class OpenApiMediaType(
    val schema: JsonSchema<*>,
) : OpenApiKonstrained<ObjectSchema> {
    override val contractSchema: ObjectSchema = schema {
        ::schema of { description = "Schema definition." }
    }
}
