package io.amichne.konditional.demo

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.configstate.ConfigurationStateFactory
import io.amichne.konditional.configstate.ConfigurationStateSerializer
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.serialization.SnapshotSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

fun main() {
    // Force initialization of feature containers by accessing their properties
    // This triggers the property delegation which registers features with the namespace
    println("[main] Initializing DemoFeatures...")
    with(DemoFeatures) {
        listOf(
            DARK_MODE, BETA_FEATURES, ANALYTICS_ENABLED, WELCOME_MESSAGE,
            THEME_COLOR, MAX_ITEMS_PER_PAGE, CACHE_TTL_SECONDS,
            DISCOUNT_PERCENTAGE, API_RATE_LIMIT,
            SSO_ENABLED, ADVANCED_ANALYTICS, CUSTOM_BRANDING, DEDICATED_SUPPORT,
        )
    }

    // Verify features are registered
    val configuration = DemoFeatures.configuration
    val snapshot = SnapshotSerializer.serialize(configuration)
    println("[main] Konfig snapshot length: ${snapshot.length}")
    if (snapshot.length < 100) {
        println("[main] WARNING: Snapshot is suspiciously small!")
        println("[main] Snapshot: $snapshot")
    } else {
        println("[main] Successfully initialized ${DemoFeatures.allFeatures().size} features")
    }

    println("[main] Starting server on port 8080...")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        // Serve static resources (compiled Kotlin/JS)
        staticResources("/static", "static")

        get("/") {
            call.respondHtml {
                renderMainPage()
            }
        }

        get("/config") {
            call.respondHtml {
                renderConfigPage()
            }
        }

        get("/api/configstate") {
            val response = ConfigurationStateFactory.from(DemoFeatures.configuration)
            call.respondText(ConfigurationStateSerializer.toJson(response), ContentType.Application.Json)
        }

        post("/api/evaluate") {
            try {
                val params = call.receiveParameters()
                val contextType = params["contextType"] ?: "base"

                val result = when (contextType) {
                    "enterprise" -> evaluateEnterpriseContext(params)
                    else -> evaluateBaseContext(params)
                }

                call.respondText(result, ContentType.Application.Json)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
}

private fun evaluateBaseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "UNITED_STATES")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0").getOrThrow()
    val stableIdHex = params["stableId"] ?: "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    val stableId = StableId.fromHex(stableIdHex)

    val context = DemoContext(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId
    )

    return buildEvaluationJson(context)
}

private fun evaluateEnterpriseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "UNITED_STATES")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0").getOrThrow()
    val stableIdHex = params["stableId"] ?: "f1e2d3c4b5a6978685746352413021ab"
    val stableId = StableId.fromHex(stableIdHex)
    val subscriptionTier = SubscriptionTier.fromString(params["tier"] ?: "FREE")
    val organizationId = params["orgId"] ?: "org-001"
    val employeeCount = params["employeeCount"]?.toIntOrNull() ?: 10

    val context = EnterpriseContext(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId,
        subscriptionTier = subscriptionTier,
        organizationId = organizationId,
        employeeCount = employeeCount
    )

    return buildEnterpriseEvaluationJson(context)
}

private fun buildEvaluationJson(context: DemoContext): String {
    val results = mutableMapOf<String, Any>()

    results["darkMode"] = DemoFeatures.DARK_MODE.evaluate(context)
    results["betaFeatures"] = DemoFeatures.BETA_FEATURES.evaluate(context)
    results["analyticsEnabled"] = DemoFeatures.ANALYTICS_ENABLED.evaluate(context)
    results["welcomeMessage"] = DemoFeatures.WELCOME_MESSAGE.evaluate(context)
    results["themeColor"] = DemoFeatures.THEME_COLOR.evaluate(context)
    results["maxItemsPerPage"] = DemoFeatures.MAX_ITEMS_PER_PAGE.evaluate(context)
    results["cacheTtlSeconds"] = DemoFeatures.CACHE_TTL_SECONDS.evaluate(context)
    results["discountPercentage"] = DemoFeatures.DISCOUNT_PERCENTAGE.evaluate(context)
    results["apiRateLimit"] = DemoFeatures.API_RATE_LIMIT.evaluate(context)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun buildEnterpriseEvaluationJson(context: EnterpriseContext): String {
    val results = mutableMapOf<String, Any>()

    // Evaluate base features with enterprise context
    results["darkMode"] = DemoFeatures.DARK_MODE.evaluate(context)
    results["betaFeatures"] = DemoFeatures.BETA_FEATURES.evaluate(context)
    results["analyticsEnabled"] = DemoFeatures.ANALYTICS_ENABLED.evaluate(context)
    results["welcomeMessage"] = DemoFeatures.WELCOME_MESSAGE.evaluate(context)
    results["themeColor"] = DemoFeatures.THEME_COLOR.evaluate(context)
    results["maxItemsPerPage"] = DemoFeatures.MAX_ITEMS_PER_PAGE.evaluate(context)
    results["cacheTtlSeconds"] = DemoFeatures.CACHE_TTL_SECONDS.evaluate(context)
    results["discountPercentage"] = DemoFeatures.DISCOUNT_PERCENTAGE.evaluate(context)
    results["apiRateLimit"] = DemoFeatures.API_RATE_LIMIT.evaluate(context)

    // Evaluate enterprise features
    results["ssoEnabled"] = DemoFeatures.SSO_ENABLED.evaluate(context)
    results["advancedAnalytics"] = DemoFeatures.ADVANCED_ANALYTICS.evaluate(context)
    results["customBranding"] = DemoFeatures.CUSTOM_BRANDING.evaluate(context)
    results["dedicatedSupport"] = DemoFeatures.DEDICATED_SUPPORT.evaluate(context)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun HTML.renderMainPage() {
    head {
        title { +"Konditional Feature Evaluation Demo" }
        style {
            unsafe {
                raw(
                    """
                    * { box-sizing: border-box; margin: 0; padding: 0; }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }

                    .container { max-width: 1400px; margin: 0 auto; }

                    .header {
                        background: white;
                        padding: 24px;
                        border-radius: 12px;
                        margin-bottom: 24px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }

                    .header h1 {
                        font-size: 28px;
                        color: #1a202c;
                    }

                    .content {
                        display: grid;
                        grid-template-columns: 400px 1fr;
                        gap: 24px;
                    }

                    .form-panel, .results-panel {
                        background: white;
                        padding: 24px;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }

                    .form-panel h2, .results-panel h2 {
                        font-size: 20px;
                        color: #1a202c;
                        margin-bottom: 20px;
                    }

                    .form-panel h3 {
                        font-size: 16px;
                        color: #2d3748;
                        margin: 16px 0 12px 0;
                    }

                    .form-group {
                        margin-bottom: 16px;
                    }

                    .form-group label {
                        display: block;
                        font-size: 14px;
                        font-weight: 500;
                        color: #4a5568;
                        margin-bottom: 6px;
                    }

                    .form-group select,
                    .form-group input {
                        width: 100%;
                        padding: 10px 12px;
                        border: 1px solid #cbd5e0;
                        border-radius: 6px;
                        font-size: 14px;
                        color: #2d3748;
                        background: white;
                    }

                    .form-group select:focus,
                    .form-group input:focus {
                        outline: none;
                        border-color: #667eea;
                        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                    }

                    .divider {
                        height: 1px;
                        background: #e2e8f0;
                        margin: 20px 0;
                    }

                    .evaluate-btn {
                        width: 100%;
                        padding: 12px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border: none;
                        border-radius: 6px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }

                    .evaluate-btn:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 6px 12px rgba(102, 126, 234, 0.3);
                    }

                    .evaluate-btn:active {
                        transform: translateY(0);
                    }

                    .results-container {
                        min-height: 200px;
                    }

                    .placeholder {
                        text-align: center;
                        color: #a0aec0;
                        padding: 60px 20px;
                    }

                    .loading {
                        text-align: center;
                        color: #667eea;
                        padding: 60px 20px;
                        font-weight: 500;
                    }

                    .error {
                        color: #e53e3e;
                        padding: 20px;
                        background: #fff5f5;
                        border: 1px solid #feb2b2;
                        border-radius: 6px;
                    }

                    .features-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                        gap: 16px;
                    }

                    .feature-card {
                        padding: 16px;
                        border-radius: 8px;
                        border: 1px solid #e2e8f0;
                        transition: all 0.2s;
                    }

                    .feature-card:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                    }

                    .feature-card.enabled {
                        background: #f0fff4;
                        border-color: #68d391;
                    }

                    .feature-card.disabled {
                        background: #fffaf0;
                        border-color: #feb2b2;
                    }

                    .feature-card.value {
                        background: #ebf8ff;
                        border-color: #90cdf4;
                    }

                    .feature-name {
                        font-size: 13px;
                        font-weight: 600;
                        color: #4a5568;
                        margin-bottom: 8px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }

                    .feature-value {
                        font-size: 16px;
                        font-weight: 500;
                        color: #1a202c;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }

                    .color-swatch {
                        display: inline-block;
                        width: 24px;
                        height: 24px;
                        border-radius: 4px;
                        border: 2px solid #e2e8f0;
                    }

                    @media (max-width: 1024px) {
                        .content {
                            grid-template-columns: 1fr;
                        }
                    }
                    """.trimIndent()
                )
            }
        }
    }
    body {
        div {
            id = "app-root"
        }

        script {
            src = "/static/demo-client.js"
        }
    }
}

private fun HTML.renderConfigPage() {
    head {
        title { +"Konditional Configuration Editor" }
        style {
            unsafe {
                raw(
                    """
                    * { box-sizing: border-box; margin: 0; padding: 0; }

                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: #f5f7fa;
                        height: 100vh;
                        overflow: hidden;
                    }

                    .config-container {
                        display: flex;
                        flex-direction: column;
                        height: 100vh;
                    }

                    .config-header {
                        background: white;
                        border-bottom: 1px solid #e2e8f0;
                        padding: 16px 24px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    }

                    .config-header h1 {
                        font-size: 20px;
                        color: #1a202c;
                        font-weight: 600;
                    }

                    .header-actions {
                        display: flex;
                        gap: 12px;
                    }

                    .btn {
                        padding: 8px 16px;
                        border-radius: 6px;
                        border: none;
                        font-size: 14px;
                        font-weight: 500;
                        cursor: pointer;
                        transition: all 0.2s;
                    }

                    .btn-primary {
                        background: #667eea;
                        color: white;
                    }

                    .btn-primary:hover {
                        background: #5a67d8;
                        transform: translateY(-1px);
                        box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
                    }

                    .btn-secondary {
                        background: white;
                        color: #4a5568;
                        border: 1px solid #cbd5e0;
                    }

                    .btn-secondary:hover {
                        background: #f7fafc;
                    }

                    .config-content {
                        display: grid;
                        grid-template-columns: 350px 1fr;
                        gap: 0;
                        height: calc(100vh - 64px);
                        overflow: hidden;
                    }

                    .sidebar {
                        background: white;
                        border-right: 1px solid #e2e8f0;
                        overflow-y: auto;
                    }

                    .main-content {
                        overflow-y: auto;
                        display: flex;
                        flex-direction: column;
                        gap: 16px;
                        padding: 16px;
                    }

                    .panel {
                        background: white;
                        border-radius: 8px;
                        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                    }

                    .sidebar .panel {
                        border-radius: 0;
                        box-shadow: none;
                        height: 100%;
                    }

                    .panel h2 {
                        font-size: 16px;
                        color: #2d3748;
                        padding: 16px 20px;
                        border-bottom: 1px solid #e2e8f0;
                        font-weight: 600;
                    }

                    .descriptor-list {
                        padding: 12px;
                    }

                    .descriptor-item {
                        padding: 12px;
                        margin-bottom: 8px;
                        background: #f7fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 6px;
                        transition: all 0.2s;
                    }

                    .descriptor-item:hover {
                        background: #edf2f7;
                        border-color: #cbd5e0;
                    }

                    .descriptor-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 8px;
                    }

                    .descriptor-key {
                        font-weight: 600;
                        font-size: 13px;
                        color: #2d3748;
                        font-family: 'Monaco', 'Courier New', monospace;
                    }

                    .descriptor-type {
                        font-size: 11px;
                        padding: 2px 8px;
                        border-radius: 4px;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }

                    .descriptor-type.boolean {
                        background: #bee3f8;
                        color: #2c5282;
                    }

                    .descriptor-type.string {
                        background: #c6f6d5;
                        color: #22543d;
                    }

                    .descriptor-type.integer,
                    .descriptor-type.number {
                        background: #fed7d7;
                        color: #742a2a;
                    }

                    .descriptor-details {
                        font-size: 12px;
                        color: #718096;
                    }

                    .detail-row {
                        display: flex;
                        gap: 8px;
                        margin-top: 4px;
                    }

                    .detail-row .label {
                        font-weight: 500;
                        color: #4a5568;
                    }

                    .fields-container {
                        padding: 20px;
                    }

                    .field-group {
                        margin-bottom: 20px;
                    }

                    .field-label {
                        display: block;
                        font-size: 14px;
                        font-weight: 500;
                        color: #2d3748;
                        margin-bottom: 8px;
                        font-family: 'Monaco', 'Courier New', monospace;
                    }

                    .field-input,
                    .field-textarea {
                        width: 100%;
                        padding: 10px 12px;
                        border: 1px solid #cbd5e0;
                        border-radius: 6px;
                        font-size: 14px;
                        color: #2d3748;
                        background: white;
                        font-family: inherit;
                    }

                    .field-textarea {
                        min-height: 100px;
                        font-family: 'Monaco', 'Courier New', monospace;
                        resize: vertical;
                    }

                    .field-input:focus,
                    .field-textarea:focus {
                        outline: none;
                        border-color: #667eea;
                        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                    }

                    .field-hint {
                        font-size: 12px;
                        color: #718096;
                        margin-top: 4px;
                    }

                    .json-viewer {
                        padding: 20px;
                        background: #1a202c;
                        color: #68d391;
                        font-family: 'Monaco', 'Courier New', monospace;
                        font-size: 12px;
                        overflow-x: auto;
                        border-radius: 0 0 8px 8px;
                        margin: 0;
                        max-height: 600px;
                    }

                    .loading,
                    .placeholder {
                        text-align: center;
                        color: #a0aec0;
                        padding: 40px 20px;
                    }

                    input[type="checkbox"] {
                        width: auto;
                        margin-right: 8px;
                        cursor: pointer;
                    }
                    """.trimIndent()
                )
            }
        }
    }
    body {
        div {
            id = "config-root"
        }

        script {
            src = "/static/demo-client.js"
        }
    }
}
