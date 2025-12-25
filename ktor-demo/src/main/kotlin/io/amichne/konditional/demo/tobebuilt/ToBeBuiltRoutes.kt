package io.amichne.konditional.demo.tobebuilt

import io.amichne.konditional.demo.DemoFeatures
import io.amichne.konditional.serialization.SnapshotSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.request.path
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.stream.createHTML
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

fun Routing.toBeBuiltRoutes() {
    get("/to-be-built") {
        call.respondToBeBuiltPage(call.request.path())
    }

    get("/to-be-built/{...}") {
        call.respondToBeBuiltPage(call.request.path())
    }

    post("/to-be-built/components/inputs/slider") {
        val params = call.receiveParameters()
        val raw = params["apiTimeoutMs"]?.toIntOrNull()
        val clamped = raw?.coerceIn(InputsDefaults.apiTimeoutMinMs, InputsDefaults.apiTimeoutMaxMs)
        val error = if (raw == null) "Invalid value" else null

        if (error == null) {
            call.response.header(
                name = "HX-Trigger",
                value = """{"toast":{"message":"Updated API timeout to ${clamped}ms"}}""",
            )
        }

        val fragment =
            createHTML().div(classes = "tbb-fieldBlock") {
                id = "tbbInputsSliderCard"
                renderInputsSliderCardBody(
                    apiTimeoutMs = clamped ?: InputsDefaults.apiTimeoutMs,
                    errorMessage = error,
                )
            }

        call.respondText(
            contentType = ContentType.Text.Html,
            status = if (error == null) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
            text = fragment,
        )
    }

    get("/to-be-built/patterns/schema-forms/flag") {
        val key = call.request.queryParameters["key"].orEmpty()
        val json = ToBeBuiltSchemaForms.findFlagJsonByKeyOrNull(key)

        val fragment =
            createHTML().div(classes = "tbb-card") {
                id = "tbbSchemaPanel"
                if (json == null) {
                    div(classes = "tbb-muted") { +"Not found: $key" }
                } else {
                    div(classes = "tbb-muted") { +"$key" }
                    div(classes = "tbb-code") { +json }
                }
            }

        call.respondText(
            contentType = ContentType.Text.Html,
            status = if (json == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
            text = fragment,
        )
    }
}

private suspend fun ApplicationCall.respondToBeBuiltPage(path: String) {
    val page = ToBeBuiltPage.fromRequestPath(path)
    val isHtmx = request.headers["HX-Request"] == "true"

    val state =
        ToBeBuiltRenderState(
            inputsApiTimeoutMs = InputsDefaults.apiTimeoutMs,
            schemaForms = ToBeBuiltSchemaForms.model(),
        )

    if (isHtmx) {
        val status = if (page is ToBeBuiltPage.NotFound) HttpStatusCode.NotFound else HttpStatusCode.OK
        respondText(
            contentType = ContentType.Text.Html,
            status = status,
            text = renderToBeBuiltContentFragment(page, state),
        )
    } else {
        respondHtml {
            renderToBeBuiltShell(page) {
                renderPageContent(page, state)
            }
        }
    }
}

private fun renderToBeBuiltContentFragment(page: ToBeBuiltPage, state: ToBeBuiltRenderState): String =
    createHTML().div(classes = "tbb-content") {
        id = "tbbContent"
        renderPageContent(page, state)
    }

private object ToBeBuiltSchemaForms {
    private val featureIdRegex = Regex("""^feature::([^:]+)::(.+)$""")

    fun model(): SchemaFormsModel =
        SchemaFormsModel(flagsByNamespace = flagsGroupedByNamespace())

    fun findFlagJsonByKeyOrNull(key: String): String? {
        val flag = flagsRaw().firstOrNull { (it["key"] as? String).orEmpty() == key } ?: return null
        val moshi = SnapshotSerializer.defaultMoshi()
        return moshi.adapter(Map::class.java).indent("  ").toJson(flag)
    }

    private fun flagsGroupedByNamespace(): Map<String, List<SchemaFlagSummary>> =
        flags()
            .groupBy(SchemaFlagSummary::namespace)
            .toSortedMap()

    private fun flags(): List<SchemaFlagSummary> =
        flagsRaw()
            .mapNotNull { raw ->
                val key = raw["key"] as? String ?: return@mapNotNull null
                val parsed = parseFeatureIdOrNull(key)
                val defaultValue = raw["defaultValue"] as? Map<*, *>
                val type = (defaultValue?.get("type") as? String).orEmpty()
                val rulesCount = (raw["rules"] as? List<*>)?.size ?: 0
                val isActive = raw["isActive"] as? Boolean ?: true

                SchemaFlagSummary(
                    key = key,
                    encodedKey = urlEncode(key),
                    namespace = parsed?.first ?: "unknown",
                    shortKey = parsed?.second ?: key,
                    type = type,
                    rulesCount = rulesCount,
                    isActive = isActive,
                )
            }

    private fun flagsRaw(): List<Map<*, *>> {
        val snapshot = SnapshotSerializer.serialize(DemoFeatures.configuration)
        val moshi = SnapshotSerializer.defaultMoshi()
        val snapshotMap = moshi.adapter(Map::class.java).fromJson(snapshot) as? Map<*, *>
        val flags = snapshotMap?.get("flags") as? List<*>
        return flags?.mapNotNull { it as? Map<*, *> }.orEmpty()
    }

    private fun parseFeatureIdOrNull(id: String): Pair<String, String>? =
        featureIdRegex.find(id)?.let { match ->
            val namespace = match.groupValues[1]
            val key = match.groupValues[2]
            namespace to key
        }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, UTF_8)
}
