@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.uiktor

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.uispec.UiNodeId
import io.amichne.konditional.uispec.UiPatchOperation
import io.amichne.konditional.uispec.UiSpec
import io.amichne.konditional.uispec.UiText
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.uiktor.html.renderFlagEditor
import io.amichne.konditional.uiktor.html.renderFlagListPage
import io.amichne.konditional.uiktor.html.renderRuleCard
import io.amichne.konditional.uiktor.html.renderRulesList
import io.amichne.konditional.uiktor.state.FlagStateService
import io.amichne.konditional.uiktor.state.createDefaultRule
import io.amichne.konditional.values.FeatureId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
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
): Unit =
    installFlagListRoute(snapshotProvider = { snapshot }, paths = paths)

fun Route.installFlagListRoute(
    snapshotProvider: () -> SerializableSnapshot,
    paths: UiRoutePaths = UiRoutePaths(),
) {
    get(paths.page) {
        val snapshot = snapshotProvider()
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

fun Route.installFlagEditorRoute(
    snapshot: SerializableSnapshot,
    paths: UiRoutePaths = UiRoutePaths(),
): Unit =
    installFlagEditorRoute(snapshotProvider = { snapshot }, paths = paths)

fun Route.installFlagEditorRoute(
    snapshotProvider: () -> SerializableSnapshot,
    paths: UiRoutePaths = UiRoutePaths(),
) {
    get("${paths.page}/flag/{key}") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val snapshot = parsedKey?.let { snapshotProvider() }
        val flag = parsedKey?.let { key -> snapshot?.flags?.find { it.key == key } }

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            flag == null -> call.respondText(
                "Flag not found: $rawKey",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderFlagEditor(flag, paths.page)
                }
            }
        }
    }
}

fun Route.installFlagMutationRoutes(
    stateService: FlagStateService,
    paths: UiRoutePaths = UiRoutePaths(),
) {
    post("${paths.page}/flag/{key}/toggle") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val updated =
            parsedKey?.let { key ->
                stateService.updateFlag(key) { flag ->
                    flag.copy(isActive = !flag.isActive)
                }
            }

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            updated == null -> call.respondText(
                "Flag not found: $rawKey",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderFlagEditor(updated, paths.page)
                }
            }
        }
    }

    post("${paths.page}/flag/{key}/rule") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val snapshot = parsedKey?.let { stateService.getSnapshot() }
        val flag = parsedKey?.let { key -> snapshot?.flags?.find { it.key == key } }
        val updatedSnapshot = flag?.let { existing ->
            stateService.addRule(existing.key, createDefaultRule(existing.defaultValue))
        }
        val updatedFlag = flag?.key?.let { key -> updatedSnapshot?.flags?.find { it.key == key } }
        val newRuleIndex = updatedFlag?.rules?.lastIndex
        val newRule = updatedFlag?.rules?.lastOrNull()

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            updatedFlag == null || newRule == null || newRuleIndex == null -> call.respondText(
                "Flag not found: $rawKey",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderRuleCard(updatedFlag, newRule, newRuleIndex, paths.page)
                }
            }
        }
    }

    post("${paths.page}/flag/{key}/rule/{index}/note") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val ruleIndex = call.parameters["index"]?.toIntOrNull()
        val note = call.receiveParameters()["note"]

        val updatedSnapshot =
            if (parsedKey == null || ruleIndex == null || note == null) {
                null
            } else {
                stateService.updateRule(parsedKey, ruleIndex) { rule ->
                    rule.copy(note = note)
                }
            }
        val updatedFlag =
            if (parsedKey == null) {
                null
            } else {
                updatedSnapshot?.flags?.find { it.key == parsedKey }
            }
        val updatedRule =
            if (updatedFlag == null || ruleIndex == null) {
                null
            } else {
                updatedFlag.rules.getOrNull(ruleIndex)
            }

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            ruleIndex == null -> call.respondText(
                "Invalid rule index",
                status = HttpStatusCode.BadRequest,
            )
            note == null -> call.respondText(
                "Missing note",
                status = HttpStatusCode.BadRequest,
            )
            updatedFlag == null || updatedRule == null -> call.respondText(
                "Rule not found",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderRuleCard(updatedFlag, updatedRule, ruleIndex, paths.page)
                }
            }
        }
    }

    post("${paths.page}/flag/{key}/rule/{index}/ramp") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val ruleIndex = call.parameters["index"]?.toIntOrNull()
        val rampValue = call.receiveParameters()["ramp"]?.toDoubleOrNull()

        val updatedSnapshot =
            if (parsedKey == null || ruleIndex == null || rampValue == null) {
                null
            } else {
                stateService.updateRule(parsedKey, ruleIndex) { rule ->
                    rule.copy(rampUp = rampValue)
                }
            }
        val updatedFlag =
            if (parsedKey == null) {
                null
            } else {
                updatedSnapshot?.flags?.find { it.key == parsedKey }
            }
        val updatedRule =
            if (updatedFlag == null || ruleIndex == null) {
                null
            } else {
                updatedFlag.rules.getOrNull(ruleIndex)
            }

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            ruleIndex == null -> call.respondText(
                "Invalid rule index",
                status = HttpStatusCode.BadRequest,
            )
            rampValue == null -> call.respondText(
                "Invalid ramp value",
                status = HttpStatusCode.BadRequest,
            )
            updatedFlag == null || updatedRule == null -> call.respondText(
                "Rule not found",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderRuleCard(updatedFlag, updatedRule, ruleIndex, paths.page)
                }
            }
        }
    }

    delete("${paths.page}/flag/{key}/rule/{index}") {
        val rawKey = call.parameters["key"]
        val parsedKey = rawKey?.let { runCatching { FeatureId.parse(it) }.getOrNull() }
        val ruleIndex = call.parameters["index"]?.toIntOrNull()
        val updatedSnapshot =
            if (parsedKey == null || ruleIndex == null) {
                null
            } else {
                stateService.deleteRule(parsedKey, ruleIndex)
            }
        val updatedFlag =
            if (parsedKey == null) {
                null
            } else {
                updatedSnapshot?.flags?.find { it.key == parsedKey }
            }

        when {
            rawKey == null -> call.respondText(
                "Missing flag key",
                status = HttpStatusCode.BadRequest,
            )
            parsedKey == null -> call.respondText(
                "Invalid flag key: $rawKey",
                status = HttpStatusCode.BadRequest,
            )
            ruleIndex == null -> call.respondText(
                "Invalid rule index",
                status = HttpStatusCode.BadRequest,
            )
            updatedFlag == null -> call.respondText(
                "Flag not found: $rawKey",
                status = HttpStatusCode.NotFound,
            )
            else -> call.respondHtml {
                body {
                    renderRulesList(updatedFlag, paths.page)
                }
            }
        }
    }
}
