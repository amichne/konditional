package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.contextualize
import io.amichne.konditional.context.feature
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.demo.DemoFeatures.ANALYTICS_ENABLED
import io.amichne.konditional.demo.DemoFeatures.API_RATE_LIMIT
import io.amichne.konditional.demo.DemoFeatures.BETA_FEATURES
import io.amichne.konditional.demo.DemoFeatures.CACHE_TTL_SECONDS
import io.amichne.konditional.demo.DemoFeatures.DARK_MODE
import io.amichne.konditional.demo.DemoFeatures.DISCOUNT_PERCENTAGE
import io.amichne.konditional.demo.DemoFeatures.MAX_ITEMS_PER_PAGE
import io.amichne.konditional.demo.DemoFeatures.THEME_COLOR
import io.amichne.konditional.demo.DemoFeatures.WELCOME_MESSAGE
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
import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

// Extension properties for display names
private val AppLocale.displayName: String
    get() = when (this) {
        AppLocale.UNITED_STATES -> "English (US)"
        AppLocale.UNITED_STATES -> "Spanish (US)"
        AppLocale.CANADA -> "English (Canada)"
        AppLocale.FRANCE -> "French (France)"
        AppLocale.INDIA -> "Hindi (India)"
        else -> error("Unsupported locale: $this")
    }

private val Platform.displayName: String
    get() = when (this) {
        Platform.IOS -> "iOS"
        Platform.ANDROID -> "Android"
        Platform.WEB -> "Web"
    }

private val SubscriptionTier.displayName: String
    get() = when (this) {
        SubscriptionTier.FREE -> "Free"
        SubscriptionTier.STARTER -> "Starter"
        SubscriptionTier.PROFESSIONAL -> "Professional"
        SubscriptionTier.ENTERPRISE -> "Enterprise"
    }

// Predefined HexId-compliant user IDs for testing
private object SampleHexIds {
    val USER_1 = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    val USER_2 = "f1e2d3c4b5a6978685746352413021ab"
    val USER_3 = "123456789abcdef0fedcba9876543210"
    val USER_4 = "deadbeefcafebabe1234567890abcdef"
    val USER_5 = "0f1e2d3c4b5a69788574635241302100"

    val all = listOf(USER_1, USER_2, USER_3, USER_4, USER_5)
    val displayNames = listOf(
        "User Alpha",
        "User Beta",
        "User Gamma",
        "User Delta",
        "User Epsilon"
    )
}

fun main() {
    // Force initialization of feature containers by accessing their properties
    // This triggers the property delegation which registers features with the namespace
    println("[main] Initializing DemoFeatures...")
    with(DemoFeatures) {
        listOf(
            DARK_MODE, BETA_FEATURES, ANALYTICS_ENABLED, WELCOME_MESSAGE,
            THEME_COLOR, MAX_ITEMS_PER_PAGE, CACHE_TTL_SECONDS,
            DISCOUNT_PERCENTAGE, API_RATE_LIMIT
        )
    }
    println("[main] Initializing EnterpriseFeatures...")
    with(EnterpriseFeatures) {
        listOf(SSO_ENABLED, ADVANCED_ANALYTICS, CUSTOM_BRANDING, DEDICATED_SUPPORT)
    }

    // Verify features are registered
    val konfig = Namespace.Global.configuration
    val snapshot = SnapshotSerializer.serialize(konfig)
    println("[main] Konfig snapshot length: ${snapshot.length}")
    if (snapshot.length < 100) {
        println("[main] WARNING: Snapshot is suspiciously small!")
        println("[main] Snapshot: $snapshot")
    } else {
        println("[main] Successfully initialized ${DemoFeatures.allFeatures().size + EnterpriseFeatures.allFeatures().size} features")
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

private fun buildRulesInfo(): String {
    println("[buildRulesInfo] Starting to build rules info")
    val moshi = com.squareup.moshi.Moshi.Builder().build()
    val adapter = moshi.adapter(Map::class.java)

    // Parse the snapshot to extract rule details
    val snapshot = SnapshotSerializer.serialize(Namespace.Global.configuration)
    println("[buildRulesInfo] Snapshot length: ${snapshot.length}")

    val snapshotData = moshi.adapter(Map::class.java).fromJson(snapshot) as? Map<*, *>
    println("[buildRulesInfo] Snapshot parsed, keys: ${snapshotData?.keys}")

    val flags = snapshotData?.get("flags")
    println("[buildRulesInfo] Flags data: $flags")
    println("[buildRulesInfo] Flags type: ${flags?.javaClass?.name}")

    val featuresData = (snapshotData?.get("flags") as? List<*>)?.map { flagData ->
        val flag = flagData as? Map<*, *>
        val key = flag?.get("key") as? String ?: "unknown"

        // Get rules array (not "values")
        val rules = flag?.get("rules") as? List<*> ?: emptyList<Any>()

        // Get defaultValue object and extract the actual value
        val defaultValueObj = flag?.get("defaultValue") as? Map<*, *>
        val defaultValue = defaultValueObj?.get("value")
        val typeStr = defaultValueObj?.get("type") as? String ?: "Unknown"

        println("[buildRulesInfo] Processing flag: key=$key, rulesCount=${rules.size}, default=$defaultValue, type=$typeStr")

        mapOf(
            "key" to key,
            "type" to typeStr.lowercase().replaceFirstChar { it.uppercase() }, // "BOOLEAN" -> "Boolean"
            "default" to defaultValue,
            "rulesCount" to rules.size,
            "hasRules" to rules.isNotEmpty(),
            "rules" to rules.map { moshi.adapter(List::class.java).toJsonValue(rules) }
        )
    } ?: emptyList()

    println("[buildRulesInfo] Features data count: ${featuresData.size}")

    val rulesMap = mapOf("features" to featuresData)
    val result = adapter.toJson(rulesMap)
    println("[buildRulesInfo] Final JSON: $result")
    return result
}

private fun evaluateBaseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "UNITED_STATES")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parseUnsafe(params["version"] ?: "1.0.0")
    val stableIdHex = params["stableId"] ?: "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    val stableId = StableId.of(stableIdHex)

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
    val stableId = StableId.of(stableIdHex)
    val subscriptionTier = SubscriptionTier.fromString(params["subscriptionTier"] ?: "FREE")
    val organizationId = params["organizationId"] ?: "org-001"
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

    // Evaluate all demo features using Context.evaluate()
    results["darkMode"] = context.evaluate(DemoFeatures.DARK_MODE)
    results["betaFeatures"] = context.evaluate(DemoFeatures.BETA_FEATURES)
    results["analyticsEnabled"] = context.evaluate(DemoFeatures.ANALYTICS_ENABLED)
    results["welcomeMessage"] = context.evaluate(DemoFeatures.WELCOME_MESSAGE)
    results["themeColor"] = context.evaluate(DemoFeatures.THEME_COLOR)
    results["maxItemsPerPage"] = context.evaluate(DemoFeatures.MAX_ITEMS_PER_PAGE)
    results["cacheTtlSeconds"] = context.evaluate(DemoFeatures.CACHE_TTL_SECONDS)
    results["discountPercentage"] = context.evaluate(DemoFeatures.DISCOUNT_PERCENTAGE)
    results["apiRateLimit"] = context.evaluate(DemoFeatures.API_RATE_LIMIT)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun buildEnterpriseEvaluationJson(context: EnterpriseContext): String {
    val results = mutableMapOf<String, Any>()

    // Evaluate base features with enterprise context
    results["darkMode"] = context.evaluate(DemoFeatures.DARK_MODE)
    results["betaFeatures"] = context.evaluate(DemoFeatures.BETA_FEATURES)
    results["analyticsEnabled"] = context.evaluate(DemoFeatures.ANALYTICS_ENABLED)
    results["welcomeMessage"] = context.evaluate(DemoFeatures.WELCOME_MESSAGE)
    results["themeColor"] = context.evaluate(DemoFeatures.THEME_COLOR)
    results["maxItemsPerPage"] = context.evaluate(DemoFeatures.MAX_ITEMS_PER_PAGE)
    results["cacheTtlSeconds"] = context.evaluate(DemoFeatures.CACHE_TTL_SECONDS)
    results["discountPercentage"] = context.evaluate(DemoFeatures.DISCOUNT_PERCENTAGE)
    results["apiRateLimit"] = context.evaluate(DemoFeatures.API_RATE_LIMIT)

    // Evaluate enterprise features
    results["ssoEnabled"] = context.evaluate(EnterpriseFeatures.SSO_ENABLED)
    results["advancedAnalytics"] = context.evaluate(EnterpriseFeatures.ADVANCED_ANALYTICS)
    results["customBranding"] = context.evaluate(EnterpriseFeatures.CUSTOM_BRANDING)
    results["dedicatedSupport"] = context.evaluate(EnterpriseFeatures.DEDICATED_SUPPORT)

    return com.squareup.moshi.Moshi.Builder().build()
        .adapter(Map::class.java)
        .toJson(results)
}

private fun HTML.renderMainPage() {
    head {
        title { +"Konditional Demo - Interactive Feature Flags" }
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
                    .features {
                        max-width: 1400px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 { font-size: 2.5em; margin-bottom: 10px; }
                    .header p { font-size: 1.1em; opacity: 0.9; }
                    .content {
                        display: grid;
                        grid-template-columns: 1fr 1fr 1fr;
                        gap: 20px;
                        padding: 30px;
                    }
                    .panel {
                        background: #f8f9fa;
                        border-radius: 8px;
                        padding: 20px;
                        border: 1px solid #e9ecef;
                    }
                    .panel h2 {
                        color: #495057;
                        margin-bottom: 20px;
                        font-size: 1.5em;
                        border-bottom: 2px solid #667eea;
                        padding-bottom: 10px;
                    }
                    .form-group {
                        margin-bottom: 15px;
                    }
                    .form-group label {
                        display: block;
                        margin-bottom: 5px;
                        color: #495057;
                        font-weight: 500;
                    }
                    .form-group input, .form-group select {
                        width: 100%;
                        padding: 10px;
                        border: 1px solid #ced4da;
                        border-radius: 4px;
                        font-size: 14px;
                    }
                    .form-group input:focus, .form-group select:focus {
                        outline: none;
                        border-color: #667eea;
                        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                    }
                    .enterprise-fields {
                        display: none;
                        margin-top: 15px;
                        padding-top: 15px;
                        border-top: 2px dashed #dee2e6;
                    }
                    .enterprise-fields.visible { display: block; }
                    .controls {
                        display: flex;
                        gap: 10px;
                        margin-top: 20px;
                    }
                    .btn {
                        padding: 12px 24px;
                        border: none;
                        border-radius: 6px;
                        font-size: 16px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.3s;
                        flex: 1;
                    }
                    .btn-primary {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                    }
                    .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); }
                    .btn-primary:disabled {
                        opacity: 0.5;
                        cursor: not-allowed;
                        transform: none;
                    }
                    .toggle-switch {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        padding: 10px;
                        background: white;
                        border-radius: 6px;
                        border: 1px solid #ced4da;
                    }
                    .switch {
                        position: relative;
                        display: inline-block;
                        width: 50px;
                        height: 24px;
                    }
                    .switch input { opacity: 0; width: 0; height: 0; }
                    .slider {
                        position: absolute;
                        cursor: pointer;
                        top: 0; left: 0; right: 0; bottom: 0;
                        background-color: #ccc;
                        transition: .4s;
                        border-radius: 24px;
                    }
                    .slider:before {
                        position: absolute;
                        content: "";
                        height: 18px;
                        width: 18px;
                        left: 3px;
                        bottom: 3px;
                        background-color: white;
                        transition: .4s;
                        border-radius: 50%;
                    }
                    input:checked + .slider { background-color: #667eea; }
                    input:checked + .slider:before { transform: translateX(26px); }
                    .features-grid {
                        display: grid;
                        gap: 10px;
                    }
                    .feature-item {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 12px;
                        background: white;
                        border-radius: 6px;
                        border-left: 4px solid #e9ecef;
                        transition: all 0.3s;
                    }
                    .feature-item.enabled { border-left-color: #10b981; background: #f0fdf4; }
                    .feature-item.disabled { border-left-color: #ef4444; background: #fef2f2; }
                    .feature-name {
                        font-weight: 600;
                        color: #495057;
                        flex: 1;
                    }
                    .feature-value {
                        padding: 4px 12px;
                        border-radius: 4px;
                        font-weight: 600;
                        font-size: 14px;
                    }
                    .feature-value.boolean {
                        background: #667eea;
                        color: white;
                    }
                    .feature-value.string {
                        background: #f59e0b;
                        color: white;
                    }
                    .feature-value.number {
                        background: #8b5cf6;
                        color: white;
                    }
                    .json-output {
                        background: #1e293b;
                        color: #e2e8f0;
                        padding: 15px;
                        border-radius: 6px;
                        font-family: 'Courier New', monospace;
                        font-size: 13px;
                        max-height: 600px;
                        overflow-y: auto;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                    .loading {
                        text-align: center;
                        padding: 40px;
                        color: #6c757d;
                    }
                    .rule-item {
                        background: white;
                        padding: 12px;
                        border-radius: 6px;
                        margin-bottom: 10px;
                        border-left: 3px solid #667eea;
                    }
                    .rule-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 8px;
                    }
                    .rule-name {
                        font-weight: 600;
                        color: #495057;
                        font-size: 14px;
                    }
                    .rule-details {
                        font-size: 12px;
                        color: #6c757d;
                        padding: 4px 0;
                    }
                    .rule-badge {
                        background: #e7f2ff;
                        color: #667eea;
                        padding: 2px 8px;
                        border-radius: 4px;
                        font-size: 11px;
                        font-weight: 600;
                        margin-right: 4px;
                    }
                    """
                )
            }
        }
    }
    body {
        div("features") {
            div("header") {
                h1 { +"🚀 Konditional Demo" }
                p { +"Interactive Feature Flags with FeatureContainer Delegation" }
            }
            div("content") {
                // Left panel - Configuration
                div("panel") {
                    h2 { +"⚙️ Configuration" }
                    form {
                        id = "contextForm"
                        div("form-group") {
                            label { +"Context Type" }
                            select {
                                id = "contextType"
                                name = "contextType"
                                option {
                                    value = "base"
                                    selected = true
                                    +"Base Context"
                                }
                                option {
                                    value = "enterprise"
                                    +"Enterprise Context (Extended)"
                                }
                            }
                        }
                        div("form-group") {
                            label { +"Locale" }
                            select {
                                id = "locale"
                                name = "locale"
                                AppLocale.entries.forEach { locale ->
                                    option {
                                        value = locale.name
                                        if (locale == AppLocale.UNITED_STATES) selected = true
                                        +locale.displayName
                                    }
                                }
                            }
                        }
                        div("form-group") {
                            label { +"Platform" }
                            select {
                                id = "platform"
                                name = "platform"
                                Platform.entries.forEach { platform ->
                                    option {
                                        value = platform.name
                                        if (platform == Platform.WEB) selected = true
                                        +platform.displayName
                                    }
                                }
                            }
                        }
                        div("form-group") {
                            label { +"App Version" }
                            input {
                                type = InputType.text
                                id = "version"
                                name = "version"
                                value = "1.0.0"
                                placeholder = "e.g., 2.5.0"
                            }
                        }
                        div("form-group") {
                            label { +"Stable ID (Test User)" }
                            select {
                                id = "stableId"
                                name = "stableId"
                                SampleHexIds.all.forEachIndexed { index, hexId ->
                                    option {
                                        value = hexId
                                        if (index == 0) selected = true
                                        +SampleHexIds.displayNames[index]
                                    }
                                }
                            }
                        }

                        // Enterprise-specific fields
                        div("enterprise-fields") {
                            id = "enterpriseFields"
                            div("form-group") {
                                label { +"Subscription Tier" }
                                select {
                                    id = "subscriptionTier"
                                    name = "subscriptionTier"
                                    SubscriptionTier.entries.forEach { tier ->
                                        option {
                                            value = tier.name
                                            if (tier == SubscriptionTier.FREE) selected = true
                                            +tier.displayName
                                        }
                                    }
                                }
                            }
                            div("form-group") {
                                label { +"Organization ID" }
                                input {
                                    type = InputType.text
                                    id = "organizationId"
                                    name = "organizationId"
                                    value = "org-001"
                                }
                            }
                            div("form-group") {
                                label { +"Employee Count" }
                                input {
                                    type = InputType.number
                                    id = "employeeCount"
                                    name = "employeeCount"
                                    value = "10"
                                }
                            }
                        }

                        div("controls") {
                            button(classes = "btn btn-primary") {
                                id = "evaluateBtn"
                                type = ButtonType.button
                                +"🔄 Evaluate Features"
                            }
                        }
                        div("toggle-switch") {
                            label("switch") {
                                input {
                                    type = InputType.checkBox
                                    id = "hotReloadToggle"
                                }
                                span("slider")
                            }
                            span { +"🔥 Hot Reload Mode" }
                        }
                    }
                }

                // Middle panel - Results
                div("panel") {
                    h2 { +"📊 Feature Evaluation Results" }
                    div {
                        id = "results"
                        div("loading") { +"Click 'Evaluate Features' to see results" }
                    }
                }

                // Right panel - Rules Configuration
                div("panel") {
                    h2 { +"🎯 Rules & Targeting" }
                    div {
                        id = "rulesPanel"
                        div("loading") { +"Loading rules..." }
                    }
                }

                // Bottom full-width panel - JSON Output
                div("panel") {
                    attributes["style"] = "grid-column: 1 / -1;"
                    h2 { +"📝 Konfig JSON Snapshot" }
                    div("json-output") {
                        id = "jsonOutput"
                        +"Loading..."
                    }
                }
            }
        }

        // Embed snapshot and rules data directly in HTML
        script {
            unsafe {
                raw(
                    """
                    window.KONDITIONAL_RULES = ${buildRulesInfo()};
                    window.KONDITIONAL_SNAPSHOT = ${SnapshotSerializer.serialize(Namespace.Global.configuration)};
                """.trimIndent()
                )
            }
        }

        // Load compiled Kotlin/JS client code
        script {
            src = "/static/demo-client.js"
        }
    }
}
