package io.amichne.konditional.demo.client

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import io.amichne.konditional.demo.net.EvaluationResponse
import org.w3c.dom.*
import org.w3c.fetch.RequestInit
import kotlin.js.json

/**
 * Simplified single-file Kotlin/JS demo client.
 * No React, no complex dependencies - just type-safe DOM manipulation.
 */
object DemoClient {
    private val scope = MainScope()

    fun init() {
        document.addEventListener("DOMContentLoaded", {
            setupUI()
        })
    }

    private fun setupUI() {
        val root = document.getElementById("app-root") as? HTMLDivElement ?: return

        root.innerHTML = """
            <div class="container">
                <header class="header">
                    <div>
                        <h1>🎯 Konditional Feature Evaluation Demo</h1>
                        <a href="/config" style="color: #667eea; text-decoration: none; font-size: 14px;">→ Open Configuration Editor</a>
                    </div>
                </header>

                <div class="content">
                    <div class="form-panel">
                        <h2>📝 Context Configuration</h2>

                        <div class="form-group">
                            <label for="contextType">Context Type</label>
                            <select id="contextType">
                                <option value="base">Base Context</option>
                                <option value="enterprise">Enterprise Context</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="locale">Locale</label>
                            <select id="locale">
                                <option value="UNITED_STATES">United States</option>
                                <option value="CANADA">Canada</option>
                                <option value="INDIA">India</option>
                                <option value="UNITED_KINGDOM">United Kingdom</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="platform">Platform</label>
                            <select id="platform">
                                <option value="WEB">Web</option>
                                <option value="IOS">iOS</option>
                                <option value="ANDROID">Android</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="version">App Version</label>
                            <input type="text" id="version" value="1.0.0" />
                        </div>

                        <div class="form-group">
                            <label for="stableId">User</label>
                            <select id="stableId">
                                <option value="a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6">User Alpha</option>
                                <option value="b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a1">User Beta</option>
                                <option value="c3d4e5f6a7b8c9d0e1f2a3b4c5d6a1b2">User Gamma</option>
                                <option value="d4e5f6a7b8c9d0e1f2a3b4c5d6a1b2c3">User Delta</option>
                                <option value="e5f6a7b8c9d0e1f2a3b4c5d6a1b2c3d4">User Epsilon</option>
                            </select>
                        </div>

                        <div id="enterpriseFields" style="display: none;">
                            <div class="divider"></div>
                            <h3>Enterprise Options</h3>

                            <div class="form-group">
                                <label for="tier">Subscription Tier</label>
                                <select id="tier">
                                    <option value="FREE">Free</option>
                                    <option value="STARTER">Starter</option>
                                    <option value="PROFESSIONAL">Professional</option>
                                    <option value="ENTERPRISE">Enterprise</option>
                                </select>
                            </div>

                            <div class="form-group">
                                <label for="orgId">Organization ID</label>
                                <input type="text" id="orgId" value="org-001" />
                            </div>

                            <div class="form-group">
                                <label for="employeeCount">Employee Count</label>
                                <input type="number" id="employeeCount" value="10" />
                            </div>
                        </div>

                        <button id="evaluateBtn" class="evaluate-btn">Evaluate Features</button>
                    </div>

                    <div class="results-panel">
                        <h2>📊 Evaluation Results</h2>
                        <div id="results" class="results-container">
                            <p class="placeholder">Click 'Evaluate Features' to see results</p>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        // Setup event listeners
        val contextTypeSelect = document.getElementById("contextType") as HTMLSelectElement
        val evaluateBtn = document.getElementById("evaluateBtn") as HTMLButtonElement

        contextTypeSelect.addEventListener("change", {
            toggleEnterpriseFields()
        })

        evaluateBtn.addEventListener("click", {
            evaluateFeatures()
        })
    }

    private fun toggleEnterpriseFields() {
        val contextType = (document.getElementById("contextType") as HTMLSelectElement).value
        val enterpriseFields = document.getElementById("enterpriseFields") as HTMLDivElement

        enterpriseFields.style.display = if (contextType == "enterprise") "block" else "none"
    }

    private fun evaluateFeatures() {
        scope.launch {
            try {
                val resultsDiv = document.getElementById("results") as HTMLDivElement
                resultsDiv.innerHTML = """<div class="loading">Evaluating...</div>"""

                val contextType = (document.getElementById("contextType") as HTMLSelectElement).value
                val locale = (document.getElementById("locale") as HTMLSelectElement).value
                val platform = (document.getElementById("platform") as HTMLSelectElement).value
                val version = (document.getElementById("version") as HTMLInputElement).value
                val stableId = (document.getElementById("stableId") as HTMLSelectElement).value

                val params = buildString {
                    append("contextType=$contextType")
                    append("&locale=$locale")
                    append("&platform=$platform")
                    append("&version=$version")
                    append("&stableId=$stableId")

                    if (contextType == "enterprise") {
                        val tier = (document.getElementById("tier") as HTMLSelectElement).value
                        val orgId = (document.getElementById("orgId") as HTMLInputElement).value
                        val employeeCount = (document.getElementById("employeeCount") as HTMLInputElement).value

                        append("&tier=$tier")
                        append("&orgId=$orgId")
                        append("&employeeCount=$employeeCount")
                    }
                }

                val response = window.fetch("/api/evaluate", json(
                    "method" to "POST",
                    "body" to params,
                    "headers" to json("Content-Type" to "application/x-www-form-urlencoded")
                ).unsafeCast<RequestInit>()).await()

                if (response.ok) {
                    val text = response.text().await()
                    val result = Json.decodeFromString<EvaluationResponse>(text)
                    renderResults(result, contextType)
                } else {
                    resultsDiv.innerHTML = """<div class="error">Evaluation failed: ${response.statusText}</div>"""
                }
            } catch (e: Exception) {
                val resultsDiv = document.getElementById("results") as HTMLDivElement
                resultsDiv.innerHTML = """<div class="error">Error: ${e.message}</div>"""
                console.error("Evaluation error:", e)
            }
        }
    }

    private fun renderResults(result: EvaluationResponse, contextType: String) {
        val resultsDiv = document.getElementById("results") as HTMLDivElement

        val html = buildString {
            append("<div class=\"features-grid\">")

            // Base features
            append(renderFeature("Dark Mode", result.darkMode))
            append(renderFeature("Beta Features", result.betaFeatures))
            append(renderFeature("Analytics Enabled", result.analyticsEnabled))
            append(renderFeature("Welcome Message", result.welcomeMessage))
            append(renderFeature("Theme Color", result.themeColor, isColor = true))
            append(renderFeature("Max Items Per Page", result.maxItemsPerPage))
            append(renderFeature("Cache TTL (seconds)", result.cacheTtlSeconds))
            append(renderFeature("Discount %", "${result.discountPercentage}%"))
            append(renderFeature("API Rate Limit", result.apiRateLimit))

            // Enterprise features
            if (contextType == "enterprise") {
                append(renderFeature("SSO Enabled", result.ssoEnabled))
                append(renderFeature("Advanced Analytics", result.advancedAnalytics))
                append(renderFeature("Custom Branding", result.customBranding))
                append(renderFeature("Dedicated Support", result.dedicatedSupport))
            }

            append("</div>")
        }

        resultsDiv.innerHTML = html
    }

    private fun renderFeature(name: String, value: Any?, isColor: Boolean = false): String {
        val displayValue = when (value) {
            is Boolean -> if (value) "Enabled" else "Disabled"
            else -> value.toString()
        }

        val statusClass = when (value) {
            is Boolean -> if (value) "enabled" else "disabled"
            else -> "value"
        }

        val colorSwatch = if (isColor && value is String) {
            """<span class="color-swatch" style="background-color: $value;"></span>"""
        } else ""

        return """
            <div class="feature-card $statusClass">
                <div class="feature-name">$name</div>
                <div class="feature-value">$colorSwatch$displayValue</div>
            </div>
        """.trimIndent()
    }
}

// Entry point - routes to appropriate client based on page
fun main() {
    when {
        document.getElementById("app-root") != null -> {
            console.log("Initializing Feature Evaluation Demo")
            DemoClient.init()
        }
        document.getElementById("config-root") != null -> {
            console.log("Initializing Configuration Editor")
            io.amichne.konditional.demo.config.ConfigEditor.init()
        }
        else -> console.warn("No known root element found")
    }
}
