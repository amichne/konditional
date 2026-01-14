@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.uispec.UiNodeId
import io.amichne.konditional.uispec.UiPatchOperation
import io.amichne.konditional.uispec.UiSpec
import io.amichne.konditional.uispec.UiText
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.uiktor.html.renderFlagListPage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.title

typealias UiHtmlBlock = HTML.() -> Unit

data class UiMessage(
    val level: UiMessageLevel,
    val text: UiText,
    val nodeId: UiNodeId? = null,
)

enum class UiMessageLevel {
    INFO,
    WARNING,
    ERROR,
}

data class UiPatchResult<S>(
    val state: S,
    val updatedNodeIds: List<UiNodeId> = emptyList(),
    val messages: List<UiMessage> = emptyList(),
)

fun interface UiPatchDecoder {
    fun decode(raw: String): List<UiPatchOperation>
}

typealias UiRenderPage<S> = (UiSpec, S, List<UiMessage>) -> UiHtmlBlock
typealias UiRenderNode<S> = (UiSpec, S, UiNodeId, List<UiMessage>) -> UiHtmlBlock
typealias UiRenderPatch<S> = (UiSpec, UiPatchResult<S>) -> UiHtmlBlock

data class UiRenderer<S>(
    val renderPage: UiRenderPage<S>,
    val renderNode: UiRenderNode<S>,
    val renderPatch: UiRenderPatch<S>,
)

interface UiSpecService<S> {
    fun loadSpec(): UiSpec
    fun loadState(): S
    fun applyPatch(state: S, patch: List<UiPatchOperation>): UiPatchResult<S>
}

data class UiRoutePaths(
    val page: String = "/config",
    val node: String = "/config/node/{id}",
    val patch: String = "/config/state",
)

data class UiRouteConfig<S>(
    val service: UiSpecService<S>,
    val renderer: UiRenderer<S>,
    val patchDecoder: UiPatchDecoder = MoshiUiPatchDecoder(),
    val paths: UiRoutePaths = UiRoutePaths(),
)

fun <S> Route.installUiRoutes(config: UiRouteConfig<S>) {
    val service = config.service
    val renderer = config.renderer
    val patchDecoder = config.patchDecoder
    val paths = config.paths

    get(paths.page) {
        val spec = service.loadSpec()
        val state = service.loadState()
        call.respondHtml(block = renderer.renderPage(spec, state, emptyList()))
    }

    get(paths.node) {
        val spec = service.loadSpec()
        val state = service.loadState()
        val nodeId = call.parameters["id"]?.let(::UiNodeId)
        if (nodeId == null) {
            call.respondText("Missing node id", status = HttpStatusCode.BadRequest)
        } else {
            call.respondHtml(block = renderer.renderNode(spec, state, nodeId, emptyList()))
        }
    }

    patch(paths.patch) {
        val spec = service.loadSpec()
        val state = service.loadState()
        val rawPatch = call.receiveText()
        val operations = patchDecoder.decode(rawPatch)
        val result = service.applyPatch(state, operations)
        call.respondHtml(block = renderer.renderPatch(spec, result))
    }
}

fun Route.installFlagListRoute(
    snapshot: SerializableSnapshot,
    paths: UiRoutePaths = UiRoutePaths(),
) {
    get(paths.page) {
        call.respondHtml {
            head {
                meta(charset = "utf-8")
                title("Feature Flags")
                link(rel = "stylesheet", href = "/static/styles.css")
                script {
                    defer = true
                    src = "https://unpkg.com/htmx.org@1.9.12"
                }
            }
            body {
                id = "main-content"
                renderFlagListPage(snapshot, paths.page)
            }
        }
    }
}
