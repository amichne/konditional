package io.amichne.konditional.server.ktor.spec

import io.amichne.konditional.configmetadata.contract.openapi.RestSurfaceOpenApiSpec
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Configuration for mounting the REST specification endpoint.
 *
 * Determinism assumptions:
 * - [specJsonProvider] must return deterministic output for identical process state.
 * - [routePath] should be stable across deployments to preserve discoverability.
 */
public data class KonditionalRestSpecRouteConfig(
    val routePath: String = "/openapi/konditional-rest-spec.json",
    val specJsonProvider: () -> String = RestSurfaceOpenApiSpec::renderJson,
)

/**
 * Registers a GET route that serves Konditional's REST specification JSON document.
 *
 * Boundary expectations:
 * - The route only serves trusted in-process data from [KonditionalRestSpecRouteConfig.specJsonProvider].
 * - No request payload is parsed.
 */
public fun Route.konditionalRestSpecRoute(config: KonditionalRestSpecRouteConfig = KonditionalRestSpecRouteConfig()) {
    get(config.routePath) {
        call.respondRestSpec(config)
    }
}

/**
 * Installs Konditional REST specification routing on an [Application].
 */
public fun Application.installKonditionalRestSpec(
    config: KonditionalRestSpecRouteConfig = KonditionalRestSpecRouteConfig(),
) {
    routing {
        konditionalRestSpecRoute(config)
    }
}

private suspend fun ApplicationCall.respondRestSpec(config: KonditionalRestSpecRouteConfig) {
    respondText(
        text = config.specJsonProvider(),
        contentType = ContentType.Application.Json,
    )
}
