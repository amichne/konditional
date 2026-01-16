package io.amichne.konditional.uiktor

import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch

/**
 * Loads and persists Configuration snapshots for the React UI.
 */
interface UiSnapshotService {
    fun load(): ConfigurationView
    fun save(configuration: Configuration)
}

data class UiReactPaths(
    val ui: String = "/ui",
    val snapshot: String = "/ui/api/snapshot",
    val patch: String = "/ui/api/patch",
)

data class UiReactRouteConfig(
    val service: UiSnapshotService,
    val paths: UiReactPaths = UiReactPaths(),
    val resourceRoot: String = "ui",
)

/**
 * Serves React assets and snapshot endpoints for the Konditional UI.
 *
 * @param config Route settings and snapshot service implementation.
 */
fun Route.installReactUiRoutes(config: UiReactRouteConfig) {
    val service = config.service
    val paths = config.paths
    val resourceRoot = config.resourceRoot

    staticResources("${paths.ui}/assets", "$resourceRoot/assets")

    get(paths.snapshot) {
        val snapshotJson = ConfigurationSnapshotCodec.encode(service.load())
        call.respondText(snapshotJson, ContentType.Application.Json)
    }

    patch(paths.patch) {
        val rawPatch = call.receiveText()
        val current = service.load()
        val result = ConfigurationSnapshotCodec.applyPatchJson(current, rawPatch)
        when (result) {
            is ParseResult.Success -> {
                service.save(result.value)
                val snapshotJson = ConfigurationSnapshotCodec.encode(result.value)
                call.respondText(snapshotJson, ContentType.Application.Json)
            }
            is ParseResult.Failure -> {
                call.respondText(result.error.message, status = HttpStatusCode.BadRequest)
            }
        }
    }


    get(paths.ui) {
        call.respondRedirect("${paths.ui}/index.html")
    }

    get("${paths.ui}/{...}") {
        val requestPath = call.request.path()
        val isApiRequest = requestPath.startsWith("${paths.ui}/api/")
        val isAssetRequest = requestPath.startsWith("${paths.ui}/assets/")
        if (isApiRequest || isAssetRequest) {
            call.respondText("Not Found", status = HttpStatusCode.NotFound)
        } else {
            call.respondReactIndex(resourceRoot)
        }
    }
}

private suspend fun ApplicationCall.respondReactIndex(resourceRoot: String) {
    val resourcePath = "$resourceRoot/index.html"
    val resourceStream = javaClass.classLoader.getResourceAsStream(resourcePath)
    if (resourceStream == null) {
        respondText("Not Found", status = HttpStatusCode.NotFound)
    } else {
        val body = resourceStream.use { it.readBytes() }
        respondBytes(body, ContentType.Text.Html)
    }
}
