package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.configstate.Editor.renderEditor
import io.amichne.konditional.demo.client.configstate.HtmlLayout.buildLayout
import io.amichne.konditional.demo.client.configstate.HtmlLayout.setStatus
import io.amichne.konditional.demo.client.configstate.HtmlLayout.showToast
import io.amichne.konditional.demo.client.configstate.HtmlLayout.ToastKind
import io.amichne.konditional.demo.client.configstate.Json.deepCopy
import io.amichne.konditional.demo.client.configstate.Json.setAtJsonPointer
import io.amichne.konditional.demo.client.configstate.Json.stableJson
import io.amichne.konditional.demo.client.configstate.Sidebar.renderFlagList
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLDivElement
import org.w3c.fetch.RequestInit
import kotlin.js.json

object Client {
    private const val endpoint: String = "/configuration-state"

    private var loaded: LoadedState? = null
    private var showAdvanced: Boolean = false

    fun init() {
        document.addEventListener("DOMContentLoaded", {
            val root = document.getElementById("configurationStateRoot") as? HTMLDivElement ?: return@addEventListener
            mount(root)
        })
    }

    private fun mount(root: HTMLDivElement) {
        root.innerHTML = ""

        val layout = buildLayout(root)
        layout.refreshBtn.addEventListener("click", { _ -> refresh(layout) })
        layout.resetBtn.addEventListener("click", { _ -> reset(layout) })
        layout.saveBtn.addEventListener("click", { _ -> save(layout) })
        layout.advancedBtn.addEventListener("click", { _ ->
            showAdvanced = !showAdvanced
            applyAdvancedToggle(layout)
            showToast(
                layout,
                ToastKind.INFO,
                if (showAdvanced) "Showing advanced fields." else "Hiding advanced fields."
            )
        })

        applyAdvancedToggle(layout)

        refresh(layout)
    }

    private fun refresh(layout: Layout) {
        setBusy(layout, busy = true)
        setStatus(layout, Status.Loading("Loading configuration-state..."))
        GlobalScope.launch {
            try {
                val response = window.fetch(endpoint, js("{}").unsafeCast<RequestInit>()).await()
                if (!response.ok) {
                    val errorText = response.text().await()
                    setStatus(
                        layout,
                        Status.Error("GET $endpoint failed: ${response.status} ${response.statusText}\n$errorText")
                    )
                    showToast(layout, ToastKind.ERROR, "Failed to load configuration-state.")
                    setBusy(layout, busy = false)
                    return@launch
                }

                val payload = response.json().await().unsafeCast<dynamic>()
                val snapshot = deepCopy(payload.currentState)

                loaded =
                    LoadedState(
                        response = payload,
                        draftSnapshot = snapshot,
                        selectedFlagIndex = 0,
                        flagFilter = "",
                        lastSavedSnapshotJson = stableJson(snapshot),
                    )

                render(layout)
                setStatus(layout, Status.Ready("Loaded."))
                setBusy(layout, busy = false)
            } catch (e: dynamic) {
                val msg = js("e.message || e.toString() || 'Unknown error'") as String
                setStatus(layout, Status.Error("GET $endpoint failed: $msg"))
                showToast(layout, ToastKind.ERROR, "Failed to load configuration-state.")
                setBusy(layout, busy = false)
            }
        }
    }

    private fun reset(layout: Layout) {
        val current = loaded ?: return
        loaded =
            current.copy(
                draftSnapshot = deepCopy(current.response.currentState),
                lastSavedSnapshotJson = stableJson(current.response.currentState),
            )
        render(layout)
        setStatus(layout, Status.Ready("Reset to server state."))
        showToast(layout, ToastKind.INFO, "Reset to server state.")
    }

    private fun save(layout: Layout) {
        val current = loaded ?: return

        setBusy(layout, busy = true)
        setStatus(layout, Status.Loading("Saving snapshot to server..."))
        GlobalScope.launch {
            try {
                val body = stableJson(current.draftSnapshot)
                val fetchOptions =
                    json(
                        "method" to "POST",
                        "body" to body,
                        "headers" to json(
                            "Content-Type" to "application/json",
                        ),
                    )

                val response = window.fetch(endpoint, fetchOptions.unsafeCast<RequestInit>()).await()
                if (!response.ok) {
                    val errorText = response.text().await()
                    setStatus(
                        layout,
                        Status.Error("POST $endpoint failed: ${response.status} ${response.statusText}\n$errorText")
                    )
                    showToast(layout, ToastKind.ERROR, "Save failed.")
                    setBusy(layout, busy = false)
                    return@launch
                }

                val payload = response.json().await().unsafeCast<dynamic>()
                val snapshot = deepCopy(payload.currentState)

                loaded =
                    current.copy(
                        response = payload,
                        draftSnapshot = snapshot,
                        lastSavedSnapshotJson = stableJson(snapshot),
                    )

                render(layout)
                setStatus(layout, Status.Ready("Saved and reloaded."))
                showToast(layout, ToastKind.SUCCESS, "Saved and reloaded.")
                setBusy(layout, busy = false)
            } catch (_: dynamic) {
                val msg = js("e.message || e.toString() || 'Unknown error'") as String
                setStatus(layout, Status.Error("POST $endpoint failed: $msg"))
                showToast(layout, ToastKind.ERROR, "Save failed.")
                setBusy(layout, busy = false)
            }
        }
    }

    internal fun render(layout: Layout) {
        val current = loaded ?: return

        layout.titleEl.textContent = "Configuration State"
        layout.subtitleEl.textContent = "Edit the active snapshot and reload it into the demo namespace."

        val dirty = stableJson(current.draftSnapshot) != current.lastSavedSnapshotJson
        layout.dirtyBadge.textContent = if (dirty) "Unsaved changes" else "Saved"
        layout.dirtyBadge.className = if (dirty) "badge badge-dirty" else "badge badge-clean"
        val isBusy = layout.frameEl.getAttribute("data-busy") == "true"
        layout.saveBtn.disabled = isBusy || !dirty
        layout.saveBtn.textContent =
            when {
                isBusy -> "Saving…"
                dirty -> "Save changes"
                else -> "No changes"
            }
        applyAdvancedToggle(layout)

        renderFlagList(layout, current)
        renderEditor(layout, current)
        renderSnapshotPreview(layout, current)
    }

    internal fun updateAtPointer(
        layout: Layout,
        pointer: String,
        value: dynamic,
    ) {
        val current = loaded ?: return
        setAtJsonPointer(current.draftSnapshot, pointer, value)
        render(layout)
    }

    internal fun updateState(update: (LoadedState) -> LoadedState) {
        loaded?.let { current ->
            loaded = update(current)
        }
    }

    internal fun supportedValues(): dynamic = loaded?.response?.supportedValues

    internal fun setStatusReady(
        layout: Layout,
        message: String,
    ) {
        setStatus(layout, Status.Ready(message))
        showToast(layout, ToastKind.SUCCESS, message)
    }

    internal fun setStatusError(
        layout: Layout,
        message: String,
    ) {
        setStatus(layout, Status.Error(message))
        showToast(layout, ToastKind.ERROR, message)
    }

    private fun applyAdvancedToggle(layout: Layout) {
        layout.frameEl.classList.toggle("hide-advanced", !showAdvanced)
        layout.advancedBtn.textContent = if (showAdvanced) "Advanced: On" else "Advanced: Off"
    }

    private fun setBusy(
        layout: Layout,
        busy: Boolean,
    ) {
        layout.frameEl.setAttribute("data-busy", busy.toString())
        layout.refreshBtn.disabled = busy
        layout.resetBtn.disabled = busy
        layout.advancedBtn.disabled = busy
    }
}
