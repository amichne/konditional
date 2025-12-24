package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.configstate.Json.stableString
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement

object HtmlLayout {

    internal fun buildLayout(root: HTMLDivElement): Layout {
        root.className = "config-state-root"

        val frame = div("frame")

        val header = div("header")
        val titleWrap = div("header-title")
        val title = div("h1")
        val subtitle = div("subtitle")
        titleWrap.appendChild(title)
        titleWrap.appendChild(subtitle)
        header.appendChild(titleWrap)

        val actions = div("header-actions")
        val refreshBtn = button("btn btn-secondary", "Refresh")
        val resetBtn = button("btn btn-secondary", "Reset")
        val saveBtn = button("btn btn-primary", "Save changes")
        val advancedBtn = button("btn btn-secondary", "Advanced: Off")
        val dirtyBadge = div("badge badge-clean").also { it.textContent = "Saved" }
        actions.appendChild(refreshBtn)
        actions.appendChild(resetBtn)
        actions.appendChild(saveBtn)
        actions.appendChild(advancedBtn)
        actions.appendChild(dirtyBadge)
        header.appendChild(actions)

        val status = div("status")
        status.textContent = "Loading..."

        val content = div("content")
        val sidebar = div("sidebar")
        val main = div("main")
        content.appendChild(sidebar)
        content.appendChild(main)

        val searchRow = div("search-row")
        val searchInput =
            (document.createElement("input") as HTMLInputElement).also {
                it.type = "search"
                it.placeholder = "Filter flags…"
                it.className = "search"
            }
        val flagCount = div("count").also { it.textContent = "" }
        searchRow.appendChild(searchInput)
        searchRow.appendChild(flagCount)
        sidebar.appendChild(searchRow)

        val flagList = div("flag-list")
        sidebar.appendChild(flagList)

        val editor = div("editor")
        main.appendChild(editor)

        val previewWrap = div("preview")
        val previewTitle = div("preview-title").also { it.textContent = "Snapshot preview (JSON)" }
        val previewText =
            (document.createElement("textarea") as HTMLTextAreaElement).also {
                it.className = "mono"
                it.readOnly = true
            }
        previewWrap.appendChild(previewTitle)
        previewWrap.appendChild(previewText)
        main.appendChild(previewWrap)

        frame.appendChild(header)
        frame.appendChild(status)
        frame.appendChild(content)
        root.appendChild(frame)
        val toastHost = div("toast-host")
        root.appendChild(toastHost)
        root.appendChild(inlineStyles())

        return Layout(
            frameEl = frame,
            titleEl = title,
            subtitleEl = subtitle,
            dirtyBadge = dirtyBadge,
            refreshBtn = refreshBtn,
            resetBtn = resetBtn,
            saveBtn = saveBtn,
            advancedBtn = advancedBtn,
            statusEl = status,
            searchInput = searchInput,
            flagCount = flagCount,
            flagList = flagList,
            editor = editor,
            snapshotPreview = previewText,
            toastHost = toastHost,
        )
    }

    internal fun setStatus(layout: Layout, status: Status) {
        val state =
            when (status) {
                is Status.Loading -> "loading"
                is Status.Ready -> "ready"
                is Status.Error -> "error"
            }
        layout.statusEl.setAttribute("data-state", state)
        layout.statusEl.className = "status"
        layout.statusEl.textContent = status.message
    }

    internal fun showToast(
        layout: Layout,
        kind: ToastKind,
        message: String,
    ) {
        val toast = div("toast toast-${kind.cssSuffix}")

        val title =
            div("toast-title").also {
                it.textContent = kind.title
            }
        val body =
            div("toast-body").also {
                it.textContent = message
            }
        val close =
            button("toast-close", "×").also {
                it.addEventListener("click", { _ ->
                    layout.toastHost.removeChild(toast)
                })
            }

        toast.appendChild(close)
        toast.appendChild(title)
        toast.appendChild(body)
        layout.toastHost.appendChild(toast)

        kotlinx.browser.window.setTimeout({
                                              if (toast.parentElement != null) {
                                                  layout.toastHost.removeChild(toast)
                                              }
                                          }, 4500)
    }

    internal enum class ToastKind(
        val cssSuffix: String,
        val title: String,
    ) {
        SUCCESS("success", "Success"),
        INFO("info", "Info"),
        ERROR("error", "Error"),
    }

    internal fun fieldSectionTitle(text: String): HTMLElement =
        div("section-title").also { it.textContent = text }

    internal fun fieldLabel(uiHints: dynamic, fallback: String): HTMLElement =
        div("field-label").also { it.textContent = stableString(uiHints.label ?: fallback) }

    internal fun fieldHelp(uiHints: dynamic): HTMLElement =
        div("field-help").also { it.textContent = stableString(uiHints.helpText ?: "") }

    internal fun div(classes: String? = null): HTMLDivElement =
        (document.createElement("div") as HTMLDivElement).also { element ->
            if (!classes.isNullOrBlank()) {
                element.className = classes
            }
        }

    internal fun button(classes: String, label: String): HTMLButtonElement =
        (document.createElement("button") as HTMLButtonElement).also {
            it.type = "button"
            it.className = classes
            it.textContent = label
        }

    private fun inlineStyles(): HTMLElement =
        (document.createElement("style") as HTMLElement).also {
            it.textContent =
                """
            .config-state-root {
              --bg: #f8fafc;
              --panel: #ffffff;
              --border: #e2e8f0;
              --text: #0f172a;
              --muted: #64748b;
              --muted-2: #94a3b8;
              --primary: #2563eb;
              --primary-2: #1d4ed8;
              --danger: #dc2626;
              --warn: #f97316;
              --shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
              --shadow-sm: 0 10px 24px rgba(15, 23, 42, 0.08);
              --ring: 0 0 0 4px rgba(37, 99, 235, 0.14);
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              color: var(--text);
              background: radial-gradient(1200px 500px at 0% 0%, rgba(37,99,235,0.10), transparent 55%),
                          radial-gradient(800px 400px at 100% 10%, rgba(249,115,22,0.10), transparent 45%),
                          var(--bg);
              border-radius: 14px;
              overflow: hidden;
            }
            .frame { border: 1px solid var(--border); border-radius: 14px; box-shadow: var(--shadow); background: var(--panel); overflow: hidden; }
            .header {
              display: flex;
              justify-content: space-between;
              gap: 16px;
              align-items: flex-start;
              padding: 18px 18px 12px;
              border-bottom: 1px solid var(--border);
              position: sticky;
              top: 0;
              background: rgba(255,255,255,0.92);
              backdrop-filter: blur(10px);
              z-index: 2;
            }
            .header-title .h1 { font-size: 20px; font-weight: 780; letter-spacing: -0.02em; }
            .subtitle { color: var(--muted); font-size: 13px; margin-top: 4px; }
            .header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
            .badge { font-size: 12px; padding: 6px 10px; border-radius: 999px; font-weight: 700; border: 1px solid var(--border); background: #f8fafc; color: var(--muted); }
            .badge-clean { background: #ecfdf5; color: #065f46; border-color: #a7f3d0; }
            .badge-dirty { background: #fff7ed; color: #9a3412; border-color: #fdba74; }
            .status { padding: 10px 18px 12px; color: var(--muted); white-space: pre-wrap; border-bottom: 1px solid var(--border); }
            .status[data-state="error"] { color: var(--danger); }
            .status[data-state="loading"] { color: var(--muted); }
            .status[data-state="loading"]::before {
              content: "";
              display: inline-block;
              width: 12px;
              height: 12px;
              border-radius: 999px;
              border: 2px solid rgba(100,116,139,0.25);
              border-top-color: rgba(37,99,235,0.9);
              margin-right: 8px;
              animation: spin 0.8s linear infinite;
              vertical-align: -2px;
            }
            @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
            .hide-advanced [data-advanced="true"] { display: none !important; }

            .content { display: grid; grid-template-columns: 380px 1fr; gap: 14px; padding: 14px; background: var(--bg); }
            .sidebar, .main { background: var(--panel); border: 1px solid var(--border); border-radius: 14px; overflow: hidden; box-shadow: var(--shadow-sm); }
            .sidebar { display: flex; flex-direction: column; min-height: 680px; }
            .search-row { display: flex; gap: 8px; align-items: center; padding: 12px; border-bottom: 1px solid var(--border); background: #fbfdff; }
            .search { flex: 1; padding: 10px 11px; border: 1px solid #cbd5e1; border-radius: 12px; outline: none; font-size: 13px; background: white; }
            .search:focus { border-color: var(--primary); box-shadow: var(--ring); }
            .search-inline { padding: 8px 10px; font-size: 12px; min-width: 160px; max-width: 280px; }
            .count { color: var(--muted-2); font-size: 12px; min-width: 84px; text-align: right; }
            .flag-list { overflow: auto; padding: 10px; display: grid; gap: 8px; }
            .flag-item { padding: 10px 10px; border: 1px solid var(--border); border-radius: 12px; cursor: pointer; background: white; transition: transform 120ms ease, box-shadow 120ms ease, border-color 120ms ease; }
            .flag-item:hover { transform: translateY(-1px); box-shadow: 0 10px 18px rgba(15,23,42,0.08); border-color: rgba(37,99,235,0.35); }
            .flag-item.selected { border-color: rgba(37,99,235,0.9); box-shadow: 0 0 0 4px rgba(37,99,235,0.12); }
            .flag-label { font-size: 12px; font-weight: 720; color: var(--text); word-break: break-word; }
            .flag-meta { margin-top: 6px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
            .pill { font-size: 11px; padding: 4px 8px; border-radius: 999px; border: 1px solid var(--border); background: #f8fafc; color: var(--muted); font-weight: 700; }
            .pill-active { background: #ecfdf5; color: #065f46; border-color: #a7f3d0; }
            .pill-inactive { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
            .pill-type { background: #eff6ff; color: #1d4ed8; border-color: #bfdbfe; }

            .main { padding: 12px; display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 12px; align-items: start; }
            .editor { border: 1px solid var(--border); border-radius: 14px; padding: 12px; background: white; }
            .editor-header { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding-bottom: 6px; border-bottom: 1px solid rgba(226,232,240,0.7); }
            .editor-title { font-size: 13px; font-weight: 780; color: var(--text); word-break: break-word; }
            .tabs { margin-top: 10px; }
            .tab-buttons { display: flex; gap: 8px; border-bottom: 1px solid var(--border); padding-bottom: 10px; }
            .tab-btn { background: #f1f5f9; border: 1px solid var(--border); padding: 8px 10px; border-radius: 12px; cursor: pointer; font-size: 12px; font-weight: 750; color: #0f172a; }
            .tab-btn:hover { border-color: rgba(37,99,235,0.45); }
            .tab-btn.active { background: #dbeafe; border-color: #93c5fd; color: #1d4ed8; }
            .tab-content { padding-top: 10px; display: grid; gap: 12px; }
            .tab-panel { display: none; gap: 12px; }
            .tab-panel.active { display: grid; }
            .section-title { font-size: 11px; color: #334155; font-weight: 800; text-transform: uppercase; letter-spacing: 0.08em; }
            .section-row { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
            .field { display: grid; gap: 6px; padding: 10px; border: 1px solid var(--border); border-radius: 14px; background: #ffffff; }
            .field-label { font-size: 12px; font-weight: 780; color: var(--text); }
            .field-help { font-size: 12px; color: var(--muted); }
            .field-controls { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 2px; }
            .field-count { color: var(--muted-2); font-size: 12px; font-weight: 750; }
            .input, .select, .mono { width: 100%; padding: 10px; border-radius: 12px; border: 1px solid #cbd5e1; outline: none; font-size: 13px; background: white; }
            .input:focus, .select:focus, .mono:focus { border-color: var(--primary); box-shadow: var(--ring); }
            .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace; min-height: 140px; }
            .button-row { display: flex; gap: 10px; justify-content: flex-end; }
            .btn { border: 1px solid transparent; border-radius: 12px; padding: 9px 12px; font-weight: 780; cursor: pointer; font-size: 13px; transition: transform 120ms ease, box-shadow 120ms ease, background 120ms ease, border-color 120ms ease; }
            .btn-sm { padding: 7px 10px; font-size: 12px; }
            .btn:active { transform: translateY(1px); }
            .btn:disabled { opacity: 0.55; cursor: not-allowed; transform: none; box-shadow: none; }
            .btn-primary { background: var(--primary); color: white; border-color: var(--primary-2); box-shadow: 0 10px 18px rgba(37,99,235,0.22); }
            .btn-primary:hover { background: var(--primary-2); }
            .btn-secondary { background: #f1f5f9; color: var(--text); border-color: var(--border); }
            .btn-secondary:hover { border-color: rgba(37,99,235,0.35); }
            .btn-danger { background: #fee2e2; color: #991b1b; border-color: #fecaca; }
            .btn-danger:hover { border-color: rgba(220,38,38,0.45); }
            .empty { color: var(--muted); padding: 10px 4px; font-size: 13px; }
            .rule-card { border: 1px solid var(--border); border-radius: 14px; padding: 10px; display: grid; gap: 10px; background: #f8fafc; }
            .rule-header { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
            .rule-title { font-weight: 800; font-size: 12px; color: var(--text); }
            .checkbox-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
            .checkbox { display: flex; gap: 8px; align-items: center; font-size: 13px; color: var(--text); }
            .preview { border: 1px solid var(--border); border-radius: 14px; overflow: hidden; background: white; }
            .preview-title { padding: 10px; background: #fbfdff; border-bottom: 1px solid var(--border); font-weight: 850; font-size: 11px; color: #334155; letter-spacing: 0.08em; text-transform: uppercase; }
            .preview textarea { border: none; border-radius: 0; min-height: 560px; }

            .toast-host { position: fixed; right: 18px; bottom: 18px; display: grid; gap: 10px; z-index: 9999; max-width: min(420px, calc(100vw - 36px)); }
            .toast {
              position: relative;
              border-radius: 14px;
              border: 1px solid var(--border);
              background: rgba(255,255,255,0.92);
              backdrop-filter: blur(12px);
              box-shadow: var(--shadow-sm);
              padding: 12px 12px 10px;
              animation: toast-in 140ms ease-out;
            }
            @keyframes toast-in { from { transform: translateY(6px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
            .toast-title { font-weight: 850; font-size: 12px; margin-bottom: 3px; }
            .toast-body { color: var(--muted); font-size: 12px; white-space: pre-wrap; }
            .toast-close {
              position: absolute;
              top: 8px;
              right: 8px;
              width: 28px;
              height: 28px;
              border-radius: 10px;
              border: 1px solid var(--border);
              background: rgba(248,250,252,0.8);
              cursor: pointer;
              font-weight: 900;
              line-height: 1;
            }
            .toast-success { border-color: rgba(16,185,129,0.35); }
            .toast-success .toast-title { color: #065f46; }
            .toast-info { border-color: rgba(37,99,235,0.35); }
            .toast-info .toast-title { color: #1d4ed8; }
            .toast-error { border-color: rgba(220,38,38,0.35); }
            .toast-error .toast-title { color: #991b1b; }

            @media (max-width: 1100px) {
              .content { grid-template-columns: 1fr; }
              .main { grid-template-columns: 1fr; }
              .preview textarea { min-height: 320px; }
            }
            """.trimIndent()
        }
}
