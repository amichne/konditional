package io.amichne.konditional.server.core.openapi

import io.amichne.konditional.server.core.surface.profile.SurfaceCapabilityProfiles
import io.amichne.konditional.server.core.surface.profile.SurfaceProfile
import io.amichne.konditional.server.core.surface.route.SurfaceParameter
import io.amichne.konditional.server.core.surface.route.SurfaceResponse
import io.amichne.konditional.server.core.surface.route.SurfaceRoute
import io.amichne.konditional.server.core.surface.route.SurfaceRouteCatalog
import io.amichne.konditional.server.core.surface.schema.SurfaceSchemaRegistry
import io.amichne.kontracts.schema.JsonSchema

internal class SurfaceOpenApiSpecBuilder(
    routes: List<SurfaceRoute> = SurfaceRouteCatalog.routes,
    private val components: Map<String, JsonSchema<*>> = SurfaceSchemaRegistry.components,
    private val info: OpenApiInfo =
        OpenApiInfo(
            title = "Konditional Surface API",
            version = "1.0.0",
            description = "Contract-first OpenAPI specification generated from route and DTO metadata.",
        ),
    private val profile: SurfaceProfile? = null,
) {
    private val filteredRoutes: List<SurfaceRoute> =
        profile?.let { selectedProfile ->
            routes.filter { route ->
                SurfaceCapabilityProfiles.supports(selectedProfile, route.capability)
            }
        } ?: routes

    private val orderedComponents: Map<String, OpenApiSchema> =
        components.entries
            .sortedBy { it.key }
            .associateTo(linkedMapOf()) { (componentName, componentSchema) ->
                componentName to OpenApiSchemaMapper.from(componentSchema)
            }

    fun build(): OpenApiDocument =
        OpenApiDocument(
            openapi = "3.0.3",
            info = info,
            paths = buildPaths(),
            components = OpenApiComponents(schemas = orderedComponents),
        )

    fun buildJson(): String = OpenApiJsonRenderer.render(build())

    private fun buildPaths(): Map<String, OpenApiPathItem> =
        filteredRoutes
            .sortedWith(compareBy(SurfaceRoute::path, { it.method.sortOrder }, SurfaceRoute::operationId))
            .groupBy(SurfaceRoute::path)
            .toSortedMap()
            .entries
            .associateTo(linkedMapOf()) { (path, groupedRoutes) ->
                path to
                    groupedRoutes
                        .sortedBy { it.method.sortOrder }
                        .fold(OpenApiPathItem()) { pathItem, route ->
                            pathItem.withOperation(route.method, buildOperation(route))
                        }
            }

    private fun buildOperation(route: SurfaceRoute): OpenApiOperation =
        OpenApiOperation(
            tags = route.tags.sorted(),
            summary = route.summary,
            description = route.description,
            operationId = route.operationId,
            responses = buildResponses(route.responses),
            parameters =
                route.parameters
                    .takeIf(List<SurfaceParameter>::isNotEmpty)
                    ?.sortedWith(compareBy({ it.location.ordinal }, SurfaceParameter::name))
                    ?.map(::buildParameter),
            requestBody =
                route.requestBody?.let { body ->
                    OpenApiRequestBody(
                        description = body.description,
                        required = body.required,
                        content = OpenApiContent(OpenApiMediaType(schema = componentRef(body.componentSchema))),
                    )
                },
        )

    private fun buildResponses(responses: List<SurfaceResponse>): Map<String, OpenApiResponse> =
        responses
            .sortedBy(SurfaceResponse::statusCode)
            .associateTo(linkedMapOf()) { response ->
                response.statusCode.toString() to buildResponse(response)
            }

    private fun buildResponse(response: SurfaceResponse): OpenApiResponse =
        OpenApiResponse(
            description = response.description,
            content =
                response.componentSchema?.let { componentName ->
                    OpenApiContent(applicationJson = OpenApiMediaType(schema = componentRef(componentName)))
                },
        )

    private fun buildParameter(parameter: SurfaceParameter): OpenApiParameter =
        OpenApiParameter(
            name = parameter.name,
            location = parameter.location.wireName,
            required = parameter.required,
            description = parameter.description,
            schema = OpenApiSchemaMapper.from(parameter.schema),
        )

    private fun componentRef(componentName: String): OpenApiSchema =
        OpenApiSchema(ref = "#/components/schemas/$componentName")
}
