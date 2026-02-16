package io.amichne.konditional.server.ktor

import io.amichne.konditional.server.core.SurfaceRestSpecification
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Registers the Konditional REST surface specification endpoint on the receiver [Route].
 *
 * Error semantics:
 * - Always returns [HttpStatusCode.OK] with OpenAPI JSON on successful route match.
 */
fun Route.konditionalSpecRoute(config: KonditionalSpecRouteConfig = KonditionalSpecRouteConfig()) {
    route(config.path) {
        get {
            call.respondText(
                text = SurfaceRestSpecification.openApiJson(),
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
            )
        }
    }
}

/**
 * Installs the Konditional spec route into an existing [Application] routing tree.
 */
fun Application.installKonditionalSpecRoute(config: KonditionalSpecRouteConfig = KonditionalSpecRouteConfig()) {
    routing {
        konditionalSpecRoute(config)
    }
}
