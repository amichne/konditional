package io.amichne.konditional.configmetadata.contract.openapi

import io.amichne.kontracts.dsl.booleanSchema as dslBooleanSchema
import io.amichne.kontracts.dsl.stringSchema as dslStringSchema
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

internal object SurfaceRouteCatalog {
    private val nonBlankText =
        dslStringSchema {
            minLength = 1
        }

    private fun booleanSchema(default: Boolean): JsonSchema<Boolean> =
        dslBooleanSchema {
            this.default = default
        }

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
                description = "Returns current snapshot state envelope.",
                tags = listOf("snapshot"),
                parameters =
                    listOf(
                        queryParameter(
                            name = "namespaceId",
                            description = "Target namespace to read.",
                            schema = nonBlankText,
                            required = true,
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
                capability = SurfaceCapability.READ_SNAPSHOT,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/snapshot",
                method = SurfaceHttpMethod.GET,
                operationId = "getNamespaceSnapshotV1",
                summary = "Read a namespace-scoped snapshot",
                description = "Returns snapshot envelope for one namespace.",
                tags = listOf("namespaces", "snapshot"),
                parameters =
                    listOf(
                        pathParameter(
                            name = "namespaceId",
                            description = "Namespace identifier.",
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
                            statusCode = 404,
                            description = "Namespace not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.READ_NAMESPACE_SNAPSHOT,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/features/{featureKey}",
                method = SurfaceHttpMethod.GET,
                operationId = "getFeatureV1",
                summary = "Read a feature",
                description = "Returns feature contract state for a namespace feature.",
                tags = listOf("features", "read"),
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
                    ),
                requestBody = null,
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 200,
                            description = "Feature envelope",
                            componentSchema = "FeatureEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 404,
                            description = "Feature not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.READ_FEATURE,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/features/{featureKey}/rules/{ruleId}",
                method = SurfaceHttpMethod.GET,
                operationId = "getRuleV1",
                summary = "Read a single rule",
                description = "Returns rule state for a namespace feature rule.",
                tags = listOf("rules", "read"),
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
                    ),
                requestBody = null,
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 200,
                            description = "Rule envelope",
                            componentSchema = "RuleEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 404,
                            description = "Rule not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.READ_RULE,
            ),
            SurfaceRoute(
                path = "/v1/snapshot",
                method = SurfaceHttpMethod.POST,
                operationId = "mutateSnapshotV1",
                summary = "Apply a snapshot mutation (legacy compatibility)",
                description = "Applies a mutation request and returns mutation codec outcome details.",
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
                capability = SurfaceCapability.MUTATE_SNAPSHOT_LEGACY_POST,
            ),
            SurfaceRoute(
                path = "/v1/snapshot",
                method = SurfaceHttpMethod.PATCH,
                operationId = "patchSnapshotV1",
                summary = "Patch snapshot state",
                description = "Applies patch-style mutation request and returns codec outcome details.",
                tags = listOf("snapshot", "mutation"),
                parameters = emptyList(),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "SnapshotMutationRequest",
                        description = "Snapshot patch payload.",
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
                            description = "Invalid patch payload",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.MUTATE_SNAPSHOT_PATCH,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}",
                method = SurfaceHttpMethod.PATCH,
                operationId = "patchNamespaceV1",
                summary = "Patch namespace metadata",
                description = "Applies namespace-level mutation and returns codec outcome details.",
                tags = listOf("namespaces", "mutation"),
                parameters =
                    listOf(
                        pathParameter(
                            name = "namespaceId",
                            description = "Namespace identifier.",
                        ),
                    ),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "NamespacePatchRequest",
                        description = "Namespace patch payload.",
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
                            description = "Invalid namespace patch request",
                            componentSchema = "ErrorEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 404,
                            description = "Namespace not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.PATCH_NAMESPACE,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/features",
                method = SurfaceHttpMethod.POST,
                operationId = "createFeatureV1",
                summary = "Create feature in namespace",
                description = "Creates a feature and returns codec outcome details.",
                tags = listOf("features", "mutation"),
                parameters =
                    listOf(
                        pathParameter(
                            name = "namespaceId",
                            description = "Namespace identifier.",
                        ),
                    ),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "FeatureCreateRequest",
                        description = "Feature creation payload.",
                    ),
                responses =
                    listOf(
                        SurfaceResponse(
                            statusCode = 201,
                            description = "Feature created",
                            componentSchema = "MutationEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 400,
                            description = "Invalid feature create request",
                            componentSchema = "ErrorEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 409,
                            description = "Feature already exists",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.CREATE_FEATURE,
            ),
            SurfaceRoute(
                path = "/v1/namespaces/{namespaceId}/features/{featureKey}",
                method = SurfaceHttpMethod.PATCH,
                operationId = "patchFeatureV1",
                summary = "Patch feature",
                description = "Mutates one feature and returns codec outcome details.",
                tags = listOf("features", "mutation"),
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
                    ),
                requestBody =
                    SurfaceRequestBody(
                        componentSchema = "FeaturePatchRequest",
                        description = "Feature patch operations.",
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
                            description = "Invalid feature patch request",
                            componentSchema = "ErrorEnvelope",
                        ),
                        SurfaceResponse(
                            statusCode = 404,
                            description = "Feature not found",
                            componentSchema = "ErrorEnvelope",
                        ),
                    ),
                capability = SurfaceCapability.PATCH_FEATURE,
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
                            schema = booleanSchema(default = false),
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
                capability = SurfaceCapability.PATCH_RULE,
            ),
        )
}
