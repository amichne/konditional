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
                val form = getFormElement("contextForm")
                val formData = FormData(form)
                val params = URLSearchParams()

                // Convert FormData to URLSearchParams
                val iterator: dynamic = js("formData.entries()")
                var entry: dynamic = iterator.next()
                while (entry.done == false) {
                    val pair = entry.value.unsafeCast<Array<String>>()
                    params.append(pair[0], pair[1])
                    entry = iterator.next()
                }

                // Make API request
                val fetchOptions = json(
                    "method" to "POST",
                    "body" to params.toString(),
                    "headers" to json(
                        "Content-Type" to "application/x-www-form-urlencoded"
                    )
                )

                val response = window.fetch("/api/evaluate", fetchOptions.unsafeCast<RequestInit>()).await()

                if (response.ok) {
                    val data = response.json().await()
                    renderResults(data.unsafeCast<Json>())
                } else {
                    val errorText = response.text().await()
                    console.error("[evaluate] Error response:", errorText)
                    showError("results", "Evaluation failed: ${response.statusText}")
                }
            } catch (e: dynamic) {
                val errorMsg = js("e.message || e.toString() || 'Unknown error'") as String
                console.error("[evaluate] Error:", errorMsg)
                showError("results", "Error: $errorMsg")
            }
        }
    }

    private fun renderResults(data: Json) {
        val results = getDivElement("results")
        val contextType = getSelectElement("contextType").value

        try {
            var html = """<div class="features-grid">"""

            // Base features
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
                html += renderFeature("SSO Enabled", data["ssoEnabled"], FeatureType.BOOLEAN)
                html += renderFeature("Advanced Analytics", data["advancedAnalytics"], FeatureType.BOOLEAN)
                html += renderFeature("Custom Branding", data["customBranding"], FeatureType.BOOLEAN)
                html += renderFeature("Dedicated Support", data["dedicatedSupport"], FeatureType.BOOLEAN)
            }

            html += """</div>"""
            results.innerHTML = html
        } catch (e: Exception) {
            console.error("[renderResults] Error:", e.message)
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
        try {
            console.log("[loadSnapshot] Reading embedded snapshot from window.KONDITIONAL_SNAPSHOT")
            val snapshotData: dynamic = js("window.KONDITIONAL_SNAPSHOT")

            if (snapshotData == null || js("typeof snapshotData === 'undefined'") == true) {
                console.error("[loadSnapshot] No embedded snapshot data found")
                getDivElement("jsonOutput").textContent = "Error: No snapshot data available"
                return
            }

            val formatted = JSON.stringify(snapshotData, null, 2)
            getDivElement("jsonOutput").textContent = formatted
            console.log("[loadSnapshot] Successfully loaded and formatted embedded snapshot")
        } catch (e: Exception) {
            console.error("[loadSnapshot] Error:", e)
            console.error("[loadSnapshot] Error message:", e.message)
            getDivElement("jsonOutput").textContent = "Error loading snapshot: ${e.message}"
        }
    }

    private fun loadRules() {
        try {
            console.log("[loadRules] Reading embedded rules from window.KONDITIONAL_RULES")
            val rulesData: dynamic = js("window.KONDITIONAL_RULES")

            if (rulesData == null || js("typeof rulesData === 'undefined'") == true) {
                console.error("[loadRules] No embedded rules data found")
                showError("rulesPanel", "Error: No rules data available")
                return
            }

            console.log("[loadRules] Rules data loaded, calling renderRules()")
            renderRules(rulesData.unsafeCast<Json>())
            console.log("[loadRules] Successfully loaded and rendered embedded rules")
        } catch (e: Exception) {
            console.error("[loadRules] Error:", e)
            console.error("[loadRules] Error message:", e.message)
            showError("rulesPanel", "Error loading rules: ${e.message}")
        }
    }

    private fun renderRules(data: Json) {
        val rulesPanel = getDivElement("rulesPanel")

        try {
            val featuresData = data["features"]

            if (featuresData == null) {
                showError("rulesPanel", "No features data received")
                return
            }

            val features = featuresData as? Array<*>

            if (features == null) {
                showError("rulesPanel", "Invalid features data format")
                return
            }

            if (features.isNotEmpty()) {
                var html = ""

                features.forEach { featureData ->
                    val feature = featureData.unsafeCast<Json>()
                    val key = feature["key"].unsafeCast<String?>() ?: "unknown"
                    val type = feature["type"].unsafeCast<String?>() ?: "Unknown"
                    val default = feature["default"]
                    val rulesCount = feature["rulesCount"].unsafeCast<Number?>()?.toInt() ?: 0
                    val hasRules = feature["hasRules"].unsafeCast<Boolean?>() ?: false

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
            } else {
                rulesPanel.innerHTML = """<div class="loading">No rules configured</div>"""
            }
        } catch (e: Exception) {
            console.error("[renderRules] Error:", e.message)
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
