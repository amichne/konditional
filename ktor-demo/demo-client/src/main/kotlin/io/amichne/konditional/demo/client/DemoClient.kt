package io.amichne.konditional.demo.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event
import org.w3c.fetch.RequestInit
import kotlin.js.Json
import kotlin.js.json

/**
 * Main client-side application for Konditional demo
 * Written in type-safe Kotlin/JS instead of raw JavaScript
 */
object DemoClient {
    private var hotReloadEnabled = false
    private var debounceTimeoutId: Int? = null

    fun init() {
        console.log("=== Initializing Konditional Demo Client (Kotlin/JS) ===")
        document.addEventListener("DOMContentLoaded", {
            console.log("[init] DOM Content Loaded - setting up event listeners")
            setupEventListeners()
            console.log("[init] Loading snapshot...")
            loadSnapshot()
            console.log("[init] Loading rules...")
            loadRules()
        })
    }

    private fun setupEventListeners() {
        val contextType = getSelectElement("contextType")
        val hotReloadToggle = getInputElement("hotReloadToggle")
        val evaluateBtn = getButtonElement("evaluateBtn")
        val form = getFormElement("contextForm")

        // Context type change handler
        contextType.addEventListener("change", { event ->
            handleContextTypeChange(event)
        })

        // Hot reload toggle handler
        hotReloadToggle.addEventListener("change", { event ->
            handleHotReloadToggle(event)
        })

        // Evaluate button handler
        evaluateBtn.addEventListener("click", { _ ->
            evaluate()
        })

        // Auto-evaluate on form changes when hot reload is enabled
        val inputs = form.querySelectorAll("input, select")
        for (i in 0 until inputs.length) {
            val input = inputs.item(i) as? HTMLElement ?: continue

            input.addEventListener("change", { _ ->
                if (hotReloadEnabled) {
                    evaluate()
                }
            })

            // Debounce text input
            if (input is HTMLInputElement && input.type == "text") {
                input.addEventListener("input", { _ ->
                    if (hotReloadEnabled) {
                        debounceTimeoutId?.let { window.clearTimeout(it) }
                        debounceTimeoutId = window.setTimeout({
                            evaluate()
                        }, 500)
                    }
                })
            }
        }
    }

    private fun handleContextTypeChange(event: Event) {
        val select = event.target as HTMLSelectElement
        val enterpriseFields = getDivElement("enterpriseFields")

        if (select.value == "enterprise") {
            enterpriseFields.classList.add("visible")
        } else {
            enterpriseFields.classList.remove("visible")
        }

        if (hotReloadEnabled) {
            evaluate()
        }
    }

    private fun handleHotReloadToggle(event: Event) {
        val checkbox = event.target as HTMLInputElement
        hotReloadEnabled = checkbox.checked

        val evaluateBtn = getButtonElement("evaluateBtn")
        evaluateBtn.disabled = hotReloadEnabled

        if (hotReloadEnabled) {
            evaluate()
        }
    }

    private fun evaluate() {
        GlobalScope.launch {
            try {
                console.log("[evaluate] Starting evaluation...")
                val form = getFormElement("contextForm")
                console.log("[evaluate] Got form element:", form)

                val formData = FormData(form)
                console.log("[evaluate] Created FormData")

                val params = URLSearchParams()
                console.log("[evaluate] Created URLSearchParams")

                // Convert FormData to URLSearchParams using proper FormData API
                try {
                    console.log("[evaluate] Converting FormData to URLSearchParams...")
                    // Use FormData.entries() API properly
                    val iterator: dynamic = js("formData.entries()")
                    var entry: dynamic = iterator.next()
                    while (entry.done == false) {
                        val pair = entry.value.unsafeCast<Array<String>>()
                        val key = pair[0]
                        val value = pair[1]
                        params.append(key, value)
                        console.log("[evaluate] Form param: $key = $value")
                        entry = iterator.next()
                    }
                    console.log("[evaluate] FormData conversion complete")
                } catch (e: Exception) {
                    console.error("[evaluate] Error converting FormData:", e)
                    console.error("[evaluate] Error details:", JSON.stringify(e.asDynamic(), null, 2))
                    throw e
                }

                console.log("[evaluate] Sending request to /api/evaluate")
                console.log("[evaluate] Request body:", params.toString())

                // Use plain JS object instead of RequestInit to avoid null values
                val fetchOptions = json(
                    "method" to "POST",
                    "body" to params.toString(),
                    "headers" to json(
                        "Content-Type" to "application/x-www-form-urlencoded"
                    )
                )

                val response = window.fetch("/api/evaluate", fetchOptions.unsafeCast<RequestInit>()).await()

                console.log("[evaluate] Response status: ${response.status}, ok: ${response.ok}")

                if (response.ok) {
                    val data = response.json().await()
                    console.log("[evaluate] Received data type:", js("typeof data"))
                    console.log("[evaluate] Data:", data)
                    renderResults(data.unsafeCast<Json>())
                } else {
                    console.error("[evaluate] Request failed with status: ${response.status}")
                    val errorText = response.text().await()
                    console.error("[evaluate] Error response:", errorText)
                    showError("results", "Evaluation failed: ${response.statusText}")
                }
            } catch (e: dynamic) {
                console.error("[evaluate] Exception caught (raw):", e)
                console.error("[evaluate] Exception type:", js("typeof e"))
                console.error("[evaluate] Exception constructor:", js("e.constructor.name"))
                console.error("[evaluate] Exception toString:", js("e.toString()"))

                // Try to get more details from the error
                val errorMsg = js("e.message || e.toString() || 'Unknown error'") as String
                console.error("[evaluate] Error message extracted:", errorMsg)

                // Log all error properties
                console.error("[evaluate] Error properties:", js("Object.keys(e)"))
                console.error("[evaluate] Error JSON:", JSON.stringify(e, null, 2))

                showError("results", "Error: $errorMsg")
            }
        }
    }

    private fun renderResults(data: Json) {
        console.log("[renderResults] Starting to render results")
        console.log("[renderResults] Data keys:", js("Object.keys(data)"))
        console.log("[renderResults] Full data:", data)

        val results = getDivElement("results")
        val contextType = getSelectElement("contextType").value
        console.log("[renderResults] Context type:", contextType)

        var html = """<div class="features-grid">"""

        try {
            // Base features
            console.log("[renderResults] Rendering base features...")
            html += renderFeature("Dark Mode", data["darkMode"], FeatureType.BOOLEAN)
            html += renderFeature("Beta Features", data["betaFeatures"], FeatureType.BOOLEAN)
            html += renderFeature("Analytics Enabled", data["analyticsEnabled"], FeatureType.BOOLEAN)
            html += renderFeature("Welcome Message", data["welcomeMessage"], FeatureType.STRING)
            html += renderFeature("Theme Color", data["themeColor"], FeatureType.STRING)
            html += renderFeature("Max Items Per Page", data["maxItemsPerPage"], FeatureType.NUMBER)
            html += renderFeature("Cache TTL (seconds)", data["cacheTtlSeconds"], FeatureType.NUMBER)
            html += renderFeature("Discount %", "${data["discountPercentage"]}%", FeatureType.NUMBER)
            html += renderFeature("API Rate Limit", data["apiRateLimit"], FeatureType.NUMBER)

            // Enterprise features
            if (contextType == "enterprise") {
                console.log("[renderResults] Rendering enterprise features...")
                html += renderFeature("SSO Enabled", data["ssoEnabled"], FeatureType.BOOLEAN)
                html += renderFeature("Advanced Analytics", data["advancedAnalytics"], FeatureType.BOOLEAN)
                html += renderFeature("Custom Branding", data["customBranding"], FeatureType.BOOLEAN)
                html += renderFeature("Dedicated Support", data["dedicatedSupport"], FeatureType.BOOLEAN)
            }

            html += """</div>"""
            results.innerHTML = html
            console.log("[renderResults] Successfully rendered results")
        } catch (e: Exception) {
            console.error("[renderResults] Error rendering:", e)
            console.error("[renderResults] Error message:", e.message)
            showError("results", "Error rendering results: ${e.message}")
        }
    }

    private fun renderFeature(name: String, value: Any?, type: FeatureType): String {
        val displayValue = when (type) {
            FeatureType.BOOLEAN -> if (value as? Boolean == true) "Enabled" else "Disabled"
            else -> value.toString()
        }

        val statusClass = when (type) {
            FeatureType.BOOLEAN -> if (value as? Boolean == true) "enabled" else "disabled"
            else -> "enabled"
        }

        return """
            <div class="feature-item $statusClass">
                <span class="feature-name">$name</span>
                <span class="feature-value ${type.cssClass}">$displayValue</span>
            </div>
        """
    }

    private fun loadSnapshot() {
        GlobalScope.launch {
            try {
                console.log("[loadSnapshot] Fetching snapshot from /api/snapshot")
                val fetchPromise: dynamic = js("fetch('/api/snapshot')")
                val response = fetchPromise.await()
                console.log("[loadSnapshot] Response status: ${response.status}")

                val json = response.text().await()
                console.log("[loadSnapshot] Received JSON length:", json.length)

                val formatted = JSON.stringify(JSON.parse(json), null, 2)
                getDivElement("jsonOutput").textContent = formatted
                console.log("[loadSnapshot] Successfully loaded and formatted snapshot")
            } catch (e: Exception) {
                console.error("[loadSnapshot] Error:", e)
                console.error("[loadSnapshot] Error message:", e.message)
                getDivElement("jsonOutput").textContent = "Error loading snapshot: ${e.message}"
            }
        }
    }

    private fun loadRules() {
        console.log("[loadRules] ===== STARTING loadRules() =====")
        GlobalScope.launch {
            try {
                console.log("[loadRules] Fetching rules from /api/rules")
                val fetchPromise: dynamic = js("fetch('/api/rules')")
                console.log("[loadRules] Fetch promise created:", fetchPromise)

                val response = fetchPromise.await()
                console.log("[loadRules] Response received:", response)
                console.log("[loadRules] Response status: ${response.status}, ok: ${response.ok}")

                if (!response.ok) {
                    console.error("[loadRules] Request failed with status: ${response.status}")
                    showError("rulesPanel", "Failed to load rules: ${response.statusText}")
                    return@launch
                }

                console.log("[loadRules] Calling response.json()...")
                val jsonPromise: dynamic = response.json()
                console.log("[loadRules] JSON promise created:", jsonPromise)

                val data = jsonPromise.await()
                console.log("[loadRules] JSON parsed successfully")
                console.log("[loadRules] Received data type:", js("typeof data"))
                console.log("[loadRules] Data:", data)
                console.log("[loadRules] Data keys:", js("Object.keys(data)"))
                console.log("[loadRules] Data.features:", js("data.features"))
                console.log("[loadRules] Data.features type:", js("typeof data.features"))
                console.log("[loadRules] Is features an Array?:", js("Array.isArray(data.features)"))
                console.log("[loadRules] Features length:", js("data.features ? data.features.length : 'undefined'"))

                console.log("[loadRules] About to call renderRules()...")
                renderRules(data.unsafeCast<Json>())
                console.log("[loadRules] renderRules() call completed")
            } catch (e: dynamic) {
                console.error("[loadRules] ===== EXCEPTION IN loadRules() =====")
                console.error("[loadRules] Exception caught (raw):", e)
                console.error("[loadRules] Exception type:", js("typeof e"))
                console.error("[loadRules] Exception toString:", js("e.toString()"))
                console.error("[loadRules] Exception message:", js("e.message || 'no message'"))
                console.error("[loadRules] Exception stack:", js("e.stack || 'no stack'"))
                showError("rulesPanel", "Error loading rules: ${js("e.message || e.toString()")}")
            }
        }
        console.log("[loadRules] ===== loadRules() GlobalScope.launch created =====")
    }

    private fun renderRules(data: Json) {
        console.log("[renderRules] ===== STARTING renderRules() =====")
        console.log("[renderRules] Received data:", data)
        console.log("[renderRules] Data type:", js("typeof data"))
        console.log("[renderRules] Data keys:", js("Object.keys(data)"))

        val rulesPanel = getDivElement("rulesPanel")
        console.log("[renderRules] Got rulesPanel element:", rulesPanel)

        try {
            console.log("[renderRules] Accessing data['features']...")
            val featuresData = data["features"]
            console.log("[renderRules] Features data:", featuresData)
            console.log("[renderRules] Features type:", js("typeof featuresData"))
            console.log("[renderRules] Features is null?:", featuresData == null)
            console.log("[renderRules] Features is undefined?:", js("featuresData === undefined"))
            console.log("[renderRules] Is Array?:", js("Array.isArray(featuresData)"))

            if (featuresData == null || js("featuresData === undefined") == true) {
                console.error("[renderRules] ERROR: featuresData is null or undefined!")
                showError("rulesPanel", "No features data received")
                return
            }

            console.log("[renderRules] Attempting to cast to Array<*>...")
            val features = featuresData as? Array<*>
            console.log("[renderRules] Cast to Array successful:", features != null)
            console.log("[renderRules] Features count:", features?.size ?: 0)

            if (features == null) {
                console.error("[renderRules] ERROR: Failed to cast featuresData to Array!")
                console.error("[renderRules] featuresData value:", featuresData)
                showError("rulesPanel", "Invalid features data format")
                return
            }

            if (features != null && features.isNotEmpty()) {
                var html = ""

                features.forEachIndexed { index, featureData ->
                    console.log("[renderRules] Processing feature #$index:", featureData)

                    val feature = featureData.unsafeCast<Json>()
                    val key = feature["key"].unsafeCast<String?>() ?: "unknown"
                    val type = feature["type"].unsafeCast<String?>() ?: "Unknown"
                    val default = feature["default"]
                    val rulesCount = feature["rulesCount"].unsafeCast<Number?>()?.toInt() ?: 0
                    val hasRules = feature["hasRules"].unsafeCast<Boolean?>() ?: false

                    console.log("[renderRules] Feature details - key: $key, type: $type, rulesCount: $rulesCount, hasRules: $hasRules")

                    val defaultDisplay = if (default is String) """"$default"""" else default.toString()
                    val rulesText = if (hasRules) {
                        "✓ $rulesCount rule(s) configured"
                    } else {
                        "✗ No rules"
                    }

                    html += """
                        <div class="rule-item">
                            <div class="rule-header">
                                <span class="rule-name">$key</span>
                                <span class="rule-badge">$type</span>
                            </div>
                            <div class="rule-details">
                                Default: $defaultDisplay<br>
                                $rulesText
                            </div>
                        </div>
                    """
                }

                rulesPanel.innerHTML = html
                console.log("[renderRules] Successfully rendered ${features.size} rules")
            } else {
                console.log("[renderRules] No features found or empty array")
                rulesPanel.innerHTML = """<div class="loading">No rules configured</div>"""
            }
        } catch (e: Exception) {
            console.error("[renderRules] Error rendering rules:", e)
            console.error("[renderRules] Error message:", e.message)
            console.error("[renderRules] Stack trace:", e.stackTraceToString())
            showError("rulesPanel", "Error rendering rules: ${e.message}")
        }
    }

    private fun showError(elementId: String, message: String) {
        val element = document.getElementById(elementId) as? HTMLDivElement
        element?.innerHTML = """<div class="loading" style="color: #ef4444;">$message</div>"""
    }

    // Type-safe element getters
    private fun getSelectElement(id: String): HTMLSelectElement =
        document.getElementById(id) as HTMLSelectElement

    private fun getInputElement(id: String): HTMLInputElement =
        document.getElementById(id) as HTMLInputElement

    private fun getButtonElement(id: String): HTMLButtonElement =
        document.getElementById(id) as HTMLButtonElement

    private fun getFormElement(id: String): HTMLFormElement =
        document.getElementById(id) as HTMLFormElement

    private fun getDivElement(id: String): HTMLDivElement =
        document.getElementById(id) as HTMLDivElement
}

enum class FeatureType(val cssClass: String) {
    BOOLEAN("boolean"),
    STRING("string"),
    NUMBER("number")
}

// External JavaScript APIs not in kotlinx-browser
external class URLSearchParams {
    fun append(name: String, value: String)
    override fun toString(): String
}

external object JSON {
    fun parse(text: String): Json
    fun stringify(obj: Json, replacer: Nothing?, space: Int): String
}

external class FormData(form: HTMLFormElement)

// Entry point
fun main() {
    DemoClient.init()
}
