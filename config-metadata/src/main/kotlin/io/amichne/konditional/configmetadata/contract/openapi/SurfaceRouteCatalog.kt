package io.amichne.konditional.configmetadata.contract.openapi

import io.amichne.kontracts.schema.JsonSchema

internal enum class SurfaceHttpMethod(
    val wireName: String,
    val sortOrder: Int,
) {
    GET(wireName = "get", sortOrder = 0),
    POST(wireName = "post", sortOrder = 1),
    PATCH(wireName = "patch", sortOrder = 2),
}

internal enum class SurfaceParameterLocation(
    val wireName: String,
) {
    PATH(wireName = "path"),
    QUERY(wireName = "query"),
}

internal data class SurfaceParameter(
    val name: String,
    val location: SurfaceParameterLocation,
    val required: Boolean,
    val description: String,
    val schema: JsonSchema<*>,
)

internal data class SurfaceRequestBody(
    val componentSchema: String,
    val description: String,
    val required: Boolean = true,
)

internal data class SurfaceResponse(
    val statusCode: Int,
    val description: String,
    val componentSchema: String?,
)

internal data class SurfaceRoute(
    val path: String,
    val method: SurfaceHttpMethod,
    val operationId: String,
    val summary: String,
    val description: String,
    val tags: List<String>,
    val parameters: List<SurfaceParameter>,
    val requestBody: SurfaceRequestBody?,
    val responses: List<SurfaceResponse>,
)

internal object SurfaceRouteCatalog {
    private val nonBlankText = JsonSchema.string(minLength = 1)

    private fun pathParameter(
        name: String,
        description: String,
    ): SurfaceParameter =
        SurfaceParameter(
            name = name,
            location = SurfaceParameterLocation.PATH,
            required = true,
            description = description,
            schema = nonBlankText,
        )

    private fun queryParameter(
        name: String,
        description: String,
        schema: JsonSchema<*>,
        required: Boolean = false,
    ): SurfaceParameter =
        SurfaceParameter(
            name = name,
            location = SurfaceParameterLocation.QUERY,
            required = required,
            description = description,
            schema = schema,
        )

    val routes: List<SurfaceRoute> =
        listOf(
            SurfaceRoute(
                path = "/v1/snapshot",
                method = SurfaceHttpMethod.GET,
                operationId = "getSnapshotV1",
                summary = "Read a namespace snapshot",
                description = "Returns current snapshot state and mutation metadata envelope.",
                tags = listOf("snapshot"),
                parameters =
                    listOf(
                        queryParameter(
                            name = "namespaceId",
                            description = "Target namespace to read.",
                            schema = nonBlankText,
                            required = true,
                        ),
                        queryParameter(
                            name = "includeMetadata",
                            description = "Whether descriptor metadata should be included.",
                            schema = JsonSchema.boolean(default = true),
                        ),
                    ),
                requestBody = null,
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 200,
                            description = "Snapshot envelope",
                            componentSchema = "SnapshotEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 400,
                            description = "Invalid query parameters",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
            ),
            SurfaceRoute(
                path = "/v1/snapshot",
                method = SurfaceHttpMethod.POST,
                operationId = "mutateSnapshotV1",
                summary = "Apply a snapshot mutation",
                description = "Applies a mutation request and returns mutation outcome metadata.",
                tags = listOf("snapshot", "mutation"),
                parameters = emptyList(),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "SnapshotMutationRequest",
                        description = "Snapshot mutation payload.",
                    ),
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 200,
                            description = "Mutation envelope",
                            componentSchema = "MutationEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 400,
                            description = "Malformed mutation payload",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}",
                method = SurfaceHttpMethod.PATCH,
                operationId = "patchRuleV1",
                summary = "Patch a single rule",
                description = "Mutates one rule and returns explicit codec outcome details.",
                tags = listOf("rules", "mutation"),
                parameters =
                    listOf(
                        pathParameter(
                            name = "namespaceId",
                            description = "Namespace identifier.",
                        ),
                        pathParameter(
                            name = "featureKey",
                            description = "Feature key identifier.",
                        ),
                        pathParameter(
                            name = "ruleId",
                            description = "Rule identifier.",
                        ),
                        queryParameter(
                            name = "dryRun",
                            description = "Validates request without persisting mutation.",
                            schema = JsonSchema.boolean(default = false),
                        ),
                    ),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "RulePatchRequest",
                        description = "Rule patch operations.",
                    ),
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 200,
                            description = "Mutation envelope",
                            componentSchema = "MutationEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 400,
                            description = "Invalid patch request",
                            componentSchema = "ErrorEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 404,
                            description = "Rule not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
            ),
        )
}
