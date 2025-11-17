package io.amichne.konditional.demo

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context.Companion.evaluate
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.serialization.SnapshotSerializer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
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

fun main() {
    // Initialize configurations
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
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

        get("/api/snapshot") {
            try {
                call.respondText(
                    SnapshotSerializer().withKonfig(Taxonomy.Global.konfig()).toJson(),
                    ContentType.Application.Json
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
}

private fun evaluateBaseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "EN_US")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0")
    val stableId = StableId.of(params["stableId"] ?: "11111111111111111111111111111111")

    val context = DemoContext(
        locale = locale,
        platform = platform,
        appVersion = version,
        stableId = stableId
    )

    return buildEvaluationJson(context)
}

private fun evaluateEnterpriseContext(params: Parameters): String {
    val locale = AppLocale.valueOf(params["locale"] ?: "EN_US")
    val platform = Platform.valueOf(params["platform"] ?: "WEB")
    val version = Version.parse(params["version"] ?: "1.0.0")
    val stableId = StableId.of(params["stableId"] ?: "22222222222222222222222222222222")
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
                    .container {
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
                        grid-template-columns: 1fr 1fr;
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
                    """
                )
            }
        }
    }
    body {
        div("container") {
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
                                option { value = "EN_US"; selected = true; +"English (US)" }
                                option { value = "EN_GB"; +"English (UK)" }
                                option { value = "FR_FR"; +"French (France)" }
                                option { value = "DE_DE"; +"German (Germany)" }
                                option { value = "ES_ES"; +"Spanish (Spain)" }
                                option { value = "JA_JP"; +"Japanese (Japan)" }
                            }
                        }
                        div("form-group") {
                            label { +"Platform" }
                            select {
                                id = "platform"
                                name = "platform"
                                option { value = "WEB"; selected = true; +"Web" }
                                option { value = "IOS"; +"iOS" }
                                option { value = "ANDROID"; +"Android" }
                                option { value = "DESKTOP"; +"Desktop" }
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
                            label { +"Stable ID (User ID)" }
                            input {
                                type = InputType.text
                                id = "stableId"
                                name = "stableId"
                                value = "user-001"
                                placeholder = "e.g., user-123"
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
                                    option { value = "FREE"; selected = true; +"Free" }
                                    option { value = "STARTER"; +"Starter" }
                                    option { value = "PROFESSIONAL"; +"Professional" }
                                    option { value = "ENTERPRISE"; +"Enterprise" }
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

                // Right panel - Results
                div("panel") {
                    h2 { +"📊 Feature Evaluation Results" }
                    div {
                        id = "results"
                        div("loading") { +"Click 'Evaluate Features' to see results" }
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

        script {
            unsafe {
                raw(
                    """
                    let hotReloadEnabled = false;

                    // Initialize
                    document.addEventListener('DOMContentLoaded', () => {
                        loadSnapshot();
                        setupEventListeners();
                    });

                    function setupEventListeners() {
                        const contextType = document.getElementById('contextType');
                        const hotReloadToggle = document.getElementById('hotReloadToggle');
                        const evaluateBtn = document.getElementById('evaluateBtn');
                        const form = document.getElementById('contextForm');

                        // Context type change
                        contextType.addEventListener('change', (e) => {
                            const enterpriseFields = document.getElementById('enterpriseFields');
                            if (e.target.value === 'enterprise') {
                                enterpriseFields.classList.add('visible');
                            } else {
                                enterpriseFields.classList.remove('visible');
                            }
                            if (hotReloadEnabled) evaluate();
                        });

                        // Hot reload toggle
                        hotReloadToggle.addEventListener('change', (e) => {
                            hotReloadEnabled = e.target.checked;
                            evaluateBtn.disabled = hotReloadEnabled;
                            if (hotReloadEnabled) evaluate();
                        });

                        // Evaluate button
                        evaluateBtn.addEventListener('click', evaluate);

                        // Auto-evaluate on input change if hot reload is enabled
                        form.querySelectorAll('input, select').forEach(input => {
                            input.addEventListener('change', () => {
                                if (hotReloadEnabled) evaluate();
                            });
                            input.addEventListener('input', () => {
                                if (hotReloadEnabled && input.type === 'text') {
                                    clearTimeout(input.timeout);
                                    input.timeout = setTimeout(() => evaluate(), 500);
                                }
                            });
                        });
                    }

                    async function evaluate() {
                        const form = document.getElementById('contextForm');
                        const formData = new FormData(form);

                        try {
                            const response = await fetch('/api/evaluate', {
                                method: 'POST',
                                body: new URLSearchParams(formData)
                            });

                            const data = await response.json();
                            renderResults(data);
                        } catch (error) {
                            console.error('Evaluation error:', error);
                            document.getElementById('results').innerHTML =
                                '<div class="loading" style="color: #ef4444;">Error: ' + error.message + '</div>';
                        }
                    }

                    function renderResults(data) {
                        const results = document.getElementById('results');
                        const contextType = document.getElementById('contextType').value;

                        let html = '<div class="features-grid">';

                        // Base features
                        html += renderFeature('Dark Mode', data.darkMode, 'boolean');
                        html += renderFeature('Beta Features', data.betaFeatures, 'boolean');
                        html += renderFeature('Analytics Enabled', data.analyticsEnabled, 'boolean');
                        html += renderFeature('Welcome Message', data.welcomeMessage, 'string');
                        html += renderFeature('Theme Color', data.themeColor, 'string');
                        html += renderFeature('Max Items Per Page', data.maxItemsPerPage, 'number');
                        html += renderFeature('Cache TTL (seconds)', data.cacheTtlSeconds, 'number');
                        html += renderFeature('Discount %', data.discountPercentage + '%', 'number');
                        html += renderFeature('API Rate Limit', data.apiRateLimit, 'number');

                        // Enterprise features
                        if (contextType === 'enterprise') {
                            html += renderFeature('SSO Enabled', data.ssoEnabled, 'boolean');
                            html += renderFeature('Advanced Analytics', data.advancedAnalytics, 'boolean');
                            html += renderFeature('Custom Branding', data.customBranding, 'boolean');
                            html += renderFeature('Dedicated Support', data.dedicatedSupport, 'boolean');
                        }

                        html += '</div>';
                        results.innerHTML = html;
                    }

                    function renderFeature(name, value, type) {
                        const isEnabled = type === 'boolean' ? value : true;
                        const displayValue = type === 'boolean' ? (value ? 'Enabled' : 'Disabled') : value;
                        const statusClass = type === 'boolean' ? (value ? 'enabled' : 'disabled') : 'enabled';

                        return `
                            <div class="feature-item ${'$'}{statusClass}">
                                <span class="feature-name">${'$'}{name}</span>
                                <span class="feature-value ${'$'}{type}">${'$'}{displayValue}</span>
                            </div>
                        `;
                    }

                    async function loadSnapshot() {
                        try {
                            const response = await fetch('/api/snapshot');
                            const json = await response.text();
                            const formatted = JSON.stringify(JSON.parse(json), null, 2);
                            document.getElementById('jsonOutput').textContent = formatted;
                        } catch (error) {
                            console.error('Snapshot error:', error);
                            document.getElementById('jsonOutput').textContent = 'Error loading snapshot: ' + error.message;
                        }
                    }
                    """
                )
            }
        }
    }
}
