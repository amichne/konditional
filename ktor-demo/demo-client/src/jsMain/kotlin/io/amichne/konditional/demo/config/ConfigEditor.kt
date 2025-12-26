package io.amichne.konditional.demo.config

import io.amichne.konditional.ui.model.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.*
import org.w3c.fetch.RequestInit
import kotlin.js.json

/**
 * Vanilla Kotlin/JS configuration editor with type-safe field editing.
 * No React - pure DOM manipulation with full schema awareness.
 */
object ConfigEditor {
    private val scope = MainScope()
    private var currentState: ConfigurationStateResponseDto? = null
    private val editedValues = mutableMapOf<String, Any?>()

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun init() {
        document.addEventListener("DOMContentLoaded", {
            setupUI()
            loadConfiguration()
        })
    }

    private fun setupUI() {
        val root = document.getElementById("config-root") as? HTMLDivElement ?: return

        root.innerHTML = """
            <div class="config-container">
                <header class="config-header">
                    <div>
                        <h1>⚙️ Configuration Editor</h1>
                        <a href="/" style="color: #667eea; text-decoration: none; font-size: 14px;">← Back to Demo</a>
                    </div>
                    <div class="header-actions">
                        <button id="refreshBtn" class="btn btn-secondary">Refresh</button>
                        <button id="saveBtn" class="btn btn-primary">Save Changes</button>
                    </div>
                </header>

                <div class="config-content">
                    <div class="sidebar">
                        <div class="panel">
                            <h2>📋 Feature Descriptors</h2>
                            <div id="descriptorCatalog" class="descriptor-list">
                                <p class="loading">Loading...</p>
                            </div>
                        </div>
                    </div>

                    <div class="main-content">
                        <div class="panel">
                            <h2>✏️ Field Bindings</h2>
                            <div id="editableFields" class="fields-container">
                                <p class="placeholder">Field editing UI coming soon - currently view-only</p>
                            </div>
                        </div>

                        <div class="panel">
                            <h2>📄 JSON Snapshot</h2>
                            <pre id="jsonSnapshot" class="json-viewer">Loading...</pre>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        // Setup event listeners
        (document.getElementById("refreshBtn") as HTMLButtonElement).onclick = {
            loadConfiguration()
        }

        (document.getElementById("saveBtn") as HTMLButtonElement).onclick = {
            saveConfiguration()
        }
    }

    private fun loadConfiguration() {
        scope.launch {
            try {
                val response = window.fetch("/api/configstate", json(
                    "method" to "GET",
                    "headers" to json("Accept" to "application/json")
                ).unsafeCast<RequestInit>()).await()

                if (response.ok) {
                    val text = response.text().await()
                    currentState = jsonCodec.decodeFromString<ConfigurationStateResponseDto>(text)
                    renderUI()
                } else {
                    showError("Failed to load configuration: ${response.statusText}")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                console.error("Load error:", e)
            }
        }
    }

    private fun saveConfiguration() {
        scope.launch {
            try {
                // TODO: Implement save endpoint
                showMessage("Save functionality coming soon!")
            } catch (e: Exception) {
                showError("Save error: ${e.message}")
            }
        }
    }

    private fun renderUI() {
        val state = currentState ?: return
        renderBindingsList(state.supportedValues.bindings)
        renderJsonSnapshot(state.currentState)
    }

    private fun renderBindingsList(bindings: Map<String, FieldTypeDto>) {
        val container = document.getElementById("descriptorCatalog") as HTMLDivElement

        if (bindings.isEmpty()) {
            container.innerHTML = """<p class="placeholder">No bindings available</p>"""
            return
        }

        val html = buildString {
            bindings.entries.sortedBy { it.key }.forEach { (path, fieldType) ->
                append("""
                    <div class="descriptor-item">
                        <div class="descriptor-header">
                            <span class="descriptor-key">$path</span>
                            <span class="descriptor-type ${fieldType.name.lowercase()}">${fieldType.name}</span>
                        </div>
                    </div>
                """.trimIndent())
            }
        }

        container.innerHTML = html
    }

    private fun renderJsonSnapshot(currentState: kotlinx.serialization.json.JsonElement) {
        val container = document.getElementById("jsonSnapshot") as HTMLPreElement
        val json = jsonCodec.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), currentState)
        container.textContent = json
    }

    private fun showError(message: String) {
        window.alert("Error: $message")
    }

    private fun showMessage(message: String) {
        window.alert(message)
    }
}
