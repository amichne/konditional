package io.amichne.konditional.uiktor

import io.amichne.konditional.uispec.JsonPointer
import io.amichne.konditional.uispec.UiArray
import io.amichne.konditional.uispec.UiBoolean
import io.amichne.konditional.uispec.UiCondition
import io.amichne.konditional.uispec.UiConditionGroup
import io.amichne.konditional.uispec.UiConditionOperator
import io.amichne.konditional.uispec.UiControlType
import io.amichne.konditional.uispec.UiDouble
import io.amichne.konditional.uispec.UiEnum
import io.amichne.konditional.uispec.UiField
import io.amichne.konditional.uispec.UiGroup
import io.amichne.konditional.uispec.UiInt
import io.amichne.konditional.uispec.UiMap
import io.amichne.konditional.uispec.UiNode
import io.amichne.konditional.uispec.UiNodeId
import io.amichne.konditional.uispec.UiNull
import io.amichne.konditional.uispec.UiObject
import io.amichne.konditional.uispec.UiPage
import io.amichne.konditional.uispec.UiSection
import io.amichne.konditional.uispec.UiSpec
import io.amichne.konditional.uispec.UiString
import io.amichne.konditional.uispec.UiTab
import io.amichne.konditional.uispec.UiTabs
import io.amichne.konditional.uispec.UiText
import io.amichne.konditional.uispec.UiTextKey
import io.amichne.konditional.uispec.UiTextLiteral
import io.amichne.konditional.uispec.UiValue
import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.body
import kotlinx.html.checkBoxInput
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h4
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.meta
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.style
import kotlinx.html.textArea
import kotlinx.html.title
import kotlinx.html.unsafe

interface UiValueProvider {
    fun read(pointer: JsonPointer): UiValue?
}

data class UiRenderSettings(
    val paths: UiRoutePaths = UiRoutePaths(),
    val rootId: String = "konditional-ui",
)

fun <S : UiValueProvider> defaultRenderer(
    settings: UiRenderSettings = UiRenderSettings(),
): UiRenderer<S> =
    UiRenderer(
        renderPage = { spec, state, messages ->
            {
                head {
                    meta(charset = "utf-8")
                    title(textFrom(spec.metadata.title).ifBlank { "Konditional" })
                    renderStyles()
                    renderHtmxScripts()
                }
                body {
                    renderPageBody(spec, state, messages, settings)
                    renderPatchHelperScript(settings)
                }
            }
        },
        renderNode = { spec, state, nodeId, messages ->
            {
                body {
                    renderNodeBody(spec, state, nodeId, messages, settings)
                }
            }
        },
        renderPatch = { spec, result ->
            {
                body {
                    renderPatchBody(spec, result, settings)
                }
            }
        },
    )

private fun <S : UiValueProvider> FlowContent.renderPageBody(
    spec: UiSpec,
    state: S,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
) {
    div {
        id = settings.rootId
        attributes["data-ui-patch-url"] = settings.paths.patch
        attributes["data-ui-node-url"] = settings.paths.node
        renderMessages(messages)
        renderNodeContent(spec, state, spec.root, messages, settings, PointerScope.empty)
    }
}

private fun HEAD.renderHtmxScripts() {
    script {
        defer = true
        src = "https://unpkg.com/htmx.org@1.9.12"
    }
    script {
        defer = true
        src = "https://unpkg.com/htmx.org@1.9.12/dist/ext/json-enc.js"
    }
}

private fun HEAD.renderStyles() {
    style {
        unsafe {
            raw(
                """
                :root {
                    --primary: #3b82f6;
                    --primary-dark: #2563eb;
                    --emerald: #10b981;
                    --blue: #3b82f6;
                    --amber: #f59e0b;
                    --orange: #f97316;
                    --purple: #a855f7;
                    --rose: #f43f5e;
                    --gray-50: #f9fafb;
                    --gray-100: #f3f4f6;
                    --gray-200: #e5e7eb;
                    --gray-300: #d1d5db;
                    --gray-400: #9ca3af;
                    --gray-500: #6b7280;
                    --gray-600: #4b5563;
                    --gray-700: #374151;
                    --gray-800: #1f2937;
                    --gray-900: #111827;
                }

                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }

                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: var(--gray-900);
                    background: var(--gray-50);
                    padding: 20px;
                }

                #konditional-ui {
                    max-width: 1400px;
                    margin: 0 auto;
                }

                h2 {
                    font-size: 28px;
                    font-weight: 700;
                    margin-bottom: 8px;
                    color: var(--gray-900);
                    letter-spacing: -0.025em;
                }

                h3 {
                    font-size: 20px;
                    font-weight: 600;
                    margin-bottom: 8px;
                    color: var(--gray-800);
                }

                h4 {
                    font-size: 16px;
                    font-weight: 600;
                    color: var(--gray-800);
                }

                p {
                    color: var(--gray-600);
                    margin-bottom: 16px;
                    font-size: 14px;
                }

                /* Page layout */
                .ui-page {
                    display: flex;
                    flex-direction: column;
                    gap: 24px;
                }

                /* Section styles */
                .ui-section {
                    display: flex;
                    flex-direction: column;
                    gap: 20px;
                }

                /* Group styles */
                .ui-group {
                    display: flex;
                    gap: 16px;
                    padding: 20px;
                    background: white;
                    border-radius: 8px;
                    border: 1px solid var(--gray-200);
                    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
                }

                .ui-group[data-ui-direction="vertical"] {
                    flex-direction: column;
                }

                .ui-group[data-ui-direction="horizontal"] {
                    flex-direction: row;
                    flex-wrap: wrap;
                }

                /* Collection styles */
                .ui-collection {
                    display: flex;
                    flex-direction: column;
                    gap: 20px;
                }

                .ui-collection-item {
                    background: white;
                    border: 2px solid var(--gray-200);
                    border-radius: 12px;
                    overflow: hidden;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
                    transition: border-color 0.2s, box-shadow 0.2s;
                }

                .ui-collection-item:hover {
                    border-color: var(--gray-300);
                    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
                }

                .ui-collection-item-header {
                    padding: 20px 24px;
                    background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
                    border-bottom: none;
                }

                .ui-collection-item-header h4 {
                    color: white;
                    font-size: 18px;
                    font-weight: 600;
                    margin: 0;
                }

                /* Field styles */
                .ui-field {
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    flex: 1;
                    min-width: 220px;
                }

                .ui-field-label {
                    font-weight: 600;
                    color: var(--gray-700);
                    font-size: 13px;
                    text-transform: uppercase;
                    letter-spacing: 0.025em;
                }

                .ui-field-description {
                    font-size: 13px;
                    color: var(--gray-500);
                    margin: -4px 0 4px 0;
                }

                .ui-field-help {
                    font-size: 12px;
                    color: var(--gray-400);
                    font-style: italic;
                }

                /* Value type badges */
                .ui-value-badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 6px;
                    padding: 4px 10px;
                    border-radius: 6px;
                    font-size: 11px;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                    border: 1px solid;
                }

                .ui-value-badge-boolean {
                    background: rgba(16, 185, 129, 0.1);
                    color: var(--emerald);
                    border-color: rgba(16, 185, 129, 0.2);
                }

                .ui-value-badge-string {
                    background: rgba(59, 130, 246, 0.1);
                    color: var(--blue);
                    border-color: rgba(59, 130, 246, 0.2);
                }

                .ui-value-badge-int,
                .ui-value-badge-double {
                    background: rgba(245, 158, 11, 0.1);
                    color: var(--amber);
                    border-color: rgba(245, 158, 11, 0.2);
                }

                .ui-value-badge-enum {
                    background: rgba(168, 85, 247, 0.1);
                    color: var(--purple);
                    border-color: rgba(168, 85, 247, 0.2);
                }

                .ui-value-badge-data_class {
                    background: rgba(244, 63, 94, 0.1);
                    color: var(--rose);
                    border-color: rgba(244, 63, 94, 0.2);
                }

                /* Form controls */
                input[type="text"],
                input[type="number"],
                select,
                textarea {
                    padding: 10px 14px;
                    border: 1.5px solid var(--gray-300);
                    border-radius: 6px;
                    font-size: 14px;
                    font-family: inherit;
                    background: white;
                    transition: all 0.2s;
                    color: var(--gray-900);
                }

                input[type="text"]:focus,
                input[type="number"]:focus,
                select:focus,
                textarea:focus {
                    outline: none;
                    border-color: var(--primary);
                    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
                }

                input[type="text"]:disabled,
                input[type="number"]:disabled,
                select:disabled,
                textarea:disabled {
                    background: var(--gray-50);
                    color: var(--gray-400);
                    cursor: not-allowed;
                    border-color: var(--gray-200);
                }

                input[type="checkbox"] {
                    width: 20px;
                    height: 20px;
                    cursor: pointer;
                    accent-color: var(--emerald);
                    border-radius: 4px;
                }

                select[multiple] {
                    min-height: 140px;
                    padding: 8px;
                }

                select[multiple] option {
                    padding: 8px 12px;
                    border-radius: 4px;
                    margin-bottom: 2px;
                }

                select[multiple] option:checked {
                    background: linear-gradient(135deg, var(--primary), var(--primary-dark));
                    color: white;
                }

                textarea {
                    resize: vertical;
                    min-height: 100px;
                    font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Courier New', monospace;
                    font-size: 13px;
                    line-height: 1.5;
                }

                /* Targeting section styles */
                .ui-targeting-section {
                    border: 1px solid var(--gray-200);
                    border-radius: 8px;
                    overflow: hidden;
                    background: white;
                }

                .ui-targeting-header {
                    padding: 16px 20px;
                    background: var(--gray-50);
                    border-bottom: 1px solid var(--gray-200);
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }

                .ui-targeting-header-icon {
                    width: 32px;
                    height: 32px;
                    border-radius: 6px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 16px;
                }

                .ui-targeting-header-icon-active {
                    background: rgba(59, 130, 246, 0.1);
                    color: var(--primary);
                }

                .ui-targeting-header-icon-inactive {
                    background: var(--gray-100);
                    color: var(--gray-400);
                }

                .ui-targeting-header-content h4 {
                    font-size: 14px;
                    font-weight: 600;
                    margin-bottom: 2px;
                }

                .ui-targeting-header-content p {
                    font-size: 12px;
                    color: var(--gray-500);
                    margin: 0;
                }

                .ui-targeting-body {
                    padding: 20px;
                }

                /* Messages */
                .ui-messages {
                    margin-bottom: 20px;
                }

                .ui-message {
                    padding: 14px 18px;
                    border-radius: 8px;
                    margin-bottom: 8px;
                    border-left: 4px solid;
                    font-size: 14px;
                }

                .ui-message-info {
                    background: rgba(59, 130, 246, 0.05);
                    color: var(--blue);
                    border-color: var(--blue);
                }

                .ui-message-warning {
                    background: rgba(245, 158, 11, 0.05);
                    color: var(--amber);
                    border-color: var(--amber);
                }

                .ui-message-error {
                    background: rgba(244, 63, 94, 0.05);
                    color: var(--rose);
                    border-color: var(--rose);
                }

                .ui-empty-state {
                    text-align: center;
                    padding: 60px 20px;
                    color: var(--gray-400);
                    background: var(--gray-50);
                    border-radius: 8px;
                    border: 2px dashed var(--gray-200);
                }

                .ui-empty-state h4 {
                    color: var(--gray-600);
                    margin-bottom: 8px;
                    font-size: 16px;
                }

                .ui-missing-node {
                    padding: 16px;
                    background: rgba(244, 63, 94, 0.05);
                    color: var(--rose);
                    border-radius: 8px;
                    border-left: 4px solid var(--rose);
                }

                /* Spacing utilities */
                [data-ui-spacing="sm"] { gap: 8px; }
                [data-ui-spacing="md"] { gap: 16px; }
                [data-ui-spacing="lg"] { gap: 24px; }
                [data-ui-spacing="xl"] { gap: 32px; }

                /* Density utilities */
                [data-ui-density="compact"] { padding: 12px; }
                [data-ui-density="comfortable"] { padding: 20px; }
                [data-ui-density="spacious"] { padding: 28px; }

                /* Collapsible sections */
                details {
                    border: 1px solid var(--gray-200);
                    border-radius: 8px;
                    background: white;
                    overflow: hidden;
                }

                details summary {
                    padding: 16px 20px;
                    cursor: pointer;
                    user-select: none;
                    list-style: none;
                    background: var(--gray-50);
                    transition: background 0.2s;
                    font-weight: 500;
                    color: var(--gray-700);
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }

                details summary:hover {
                    background: var(--gray-100);
                }

                details summary::after {
                    content: '▶';
                    transition: transform 0.2s;
                    color: var(--gray-400);
                }

                details[open] summary::after {
                    transform: rotate(90deg);
                }

                details[open] summary {
                    border-bottom: 1px solid var(--gray-200);
                    background: white;
                }

                details > div {
                    padding: 20px;
                }

                /* Section headers within collection items */
                .ui-section-header {
                    padding: 16px 24px;
                    background: var(--gray-50);
                    border-top: 1px solid var(--gray-200);
                    border-bottom: 1px solid var(--gray-200);
                }

                .ui-section-header h3 {
                    font-size: 15px;
                    font-weight: 600;
                    color: var(--gray-700);
                    margin: 0;
                }

                .ui-section-header p {
                    font-size: 13px;
                    color: var(--gray-500);
                    margin: 4px 0 0 0;
                }

                /* Content sections */
                .ui-section-content {
                    padding: 24px;
                }
                """.trimIndent(),
            )
        }
    }
}

private fun FlowContent.renderPatchHelperScript(settings: UiRenderSettings) {
    script {
        type = "text/javascript"
        unsafe {
            raw(
                """
                (function() {
                  const root = document.getElementById("${settings.rootId}");
                  if (!root) return;
                  const patchUrl = root.dataset.uiPatchUrl;
                  if (!patchUrl) return;

                  function buildValue(el) {
                    const control = el.dataset.uiControl || '';
                    const kind = el.dataset.uiKind || '';
                    if (el.type === 'checkbox') return el.checked;
                    if (el.multiple) {
                      return Array.from(el.selectedOptions || []).map(option => option.value);
                    }
                    const raw = typeof el.value === 'string' ? el.value : '';
                    if (kind === 'int') {
                      const n = parseInt(raw, 10);
                      return Number.isNaN(n) ? 0 : n;
                    }
                    if (kind === 'double' || kind === 'percent') {
                      const n = parseFloat(raw);
                      return Number.isNaN(n) ? 0 : n;
                    }
                    if (control === 'json' || control === 'key_value') {
                      try {
                        return JSON.parse(raw);
                      } catch (e) {
                        return raw;
                      }
                    }
                    return raw;
                  }

                  window.konditionalUiPatchValue = buildValue;
                  if (window.htmx) return;

                  async function sendPatch(pointer, value) {
                    const payload = JSON.stringify([{ op: 'replace', path: pointer, value }]);
                    const response = await fetch(patchUrl, {
                      method: 'PATCH',
                      headers: { 'Content-Type': 'application/json' },
                      body: payload
                    });
                    if (!response.ok) return;
                    const html = await response.text();
                    applyPatch(html);
                  }

                  function applyPatch(html) {
                    const doc = new DOMParser().parseFromString(html, 'text/html');
                    const container = doc.querySelector('[data-ui-patch-result]') || doc.body;
                    const nodes = container.querySelectorAll('[data-ui-node]');
                    if (nodes.length === 0) {
                      root.innerHTML = container.innerHTML;
                      return;
                    }
                    nodes.forEach(node => {
                      const id = node.getAttribute('data-ui-node');
                      if (!id) return;
                      const current = root.querySelector('[data-ui-node="' + id + '"]');
                      if (current) {
                        current.replaceWith(node);
                      }
                    });
                  }

                  function handleEvent(event) {
                    const el = event.target;
                    if (!(el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || el instanceof HTMLSelectElement)) {
                      return;
                    }
                    const pointer = el.getAttribute('data-ui-pointer') || el.name;
                    if (!pointer) return;
                    sendPatch(pointer, buildValue(el));
                  }

                  root.addEventListener('change', handleEvent, true);
                })();
                """.trimIndent(),
            )
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderNodeBody(
    spec: UiSpec,
    state: S,
    nodeId: UiNodeId,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
) {
    val node = findNode(spec.root, nodeId)
    if (node == null) {
        div {
            classes = setOf("ui-missing-node")
            +"Unknown node: ${nodeId.value}"
        }
    } else {
        renderNodeContent(spec, state, node, messages, settings, PointerScope.empty)
    }
}

private fun <S : UiValueProvider> FlowContent.renderPatchBody(
    spec: UiSpec,
    result: UiPatchResult<S>,
    settings: UiRenderSettings,
) {
    val nodeIds = result.updatedNodeIds.ifEmpty { listOf(spec.root.id) }
    div {
        attributes["data-ui-patch-result"] = "true"
        renderMessages(result.messages)
        nodeIds.forEach { nodeId ->
            val node = findNode(spec.root, nodeId)
            if (node == null) {
                div {
                    classes = setOf("ui-missing-node")
                    +"Unknown node: ${nodeId.value}"
                }
            } else {
                renderNodeContent(spec, result.state, node, result.messages, settings, PointerScope.empty)
            }
        }
    }
}

private fun FlowContent.renderMessages(messages: List<UiMessage>) {
    if (messages.isNotEmpty()) {
        div {
            classes = setOf("ui-messages")
            messages.forEach { message ->
                div {
                    classes = setOf("ui-message", "ui-message-${message.level.name.lowercase()}")
                    +textFrom(message.text)
                }
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderNodeContent(
    spec: UiSpec,
    state: S,
    node: UiNode,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) =
    when (node) {
        is UiPage -> renderPageNode(spec, state, node, messages, settings, scope)
        is UiSection -> renderSectionNode(spec, state, node, messages, settings, scope)
        is UiGroup -> renderGroupNode(spec, state, node, messages, settings, scope)
        is UiTabs -> renderTabsNode(spec, state, node, messages, settings, scope)
        is UiField -> renderFieldNode(state, node, settings, scope)
        is io.amichne.konditional.uispec.UiCollection ->
            renderCollectionNode(spec, state, node, messages, settings, scope)
    }

private fun <S : UiValueProvider> FlowContent.renderPageNode(
    spec: UiSpec,
    state: S,
    node: UiPage,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        div {
            classes = setOf("ui-page")
            attributes["data-ui-node"] = node.id.value
            applyLayout(node.layout)
            renderMeta(node)
            node.children.forEach { child ->
                renderNodeContent(spec, state, child, messages, settings, scope)
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderSectionNode(
    spec: UiSpec,
    state: S,
    node: UiSection,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        div {
            classes = setOf("ui-section")
            attributes["data-ui-node"] = node.id.value
            applyLayout(node.layout)

            if (node.meta.title != null || node.meta.description != null) {
                div {
                    classes = setOf("ui-section-header")
                    renderMeta(node)
                }
            }

            div {
                classes = setOf("ui-section-content")
                node.children.forEach { child ->
                    renderNodeContent(spec, state, child, messages, settings, scope)
                }
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderGroupNode(
    spec: UiSpec,
    state: S,
    node: UiGroup,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        div {
            classes = setOf("ui-group")
            attributes["data-ui-node"] = node.id.value
            applyLayout(node.layout)
            renderMeta(node)
            node.children.forEach { child ->
                renderNodeContent(spec, state, child, messages, settings, scope)
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderTabsNode(
    spec: UiSpec,
    state: S,
    node: UiTabs,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        div {
            classes = setOf("ui-tabs")
            attributes["data-ui-node"] = node.id.value
            renderMeta(node)
            node.tabs.forEach { tab ->
                renderTab(spec, state, tab, messages, settings, scope)
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderTab(
    spec: UiSpec,
    state: S,
    tab: UiTab,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (tab.isVisible(state, scope)) {
        div {
            classes = setOf("ui-tab")
            attributes["data-ui-tab"] = tab.id.value
            renderTabMeta(tab)
            tab.children.forEach { child ->
                renderNodeContent(spec, state, child, messages, settings, scope)
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderCollectionNode(
    spec: UiSpec,
    state: S,
    node: io.amichne.konditional.uispec.UiCollection,
    messages: List<UiMessage>,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        val resolvedPointer = resolvePointer(node.itemsPointer, scope)
        val items = state.read(resolvedPointer).asArrayValues()
        div {
            classes = setOf("ui-collection")
            attributes["data-ui-node"] = node.id.value
            attributes["data-ui-items-pointer"] = resolvedPointer.value
            renderMeta(node)
            if (items.isEmpty()) {
                node.emptyState?.let { emptyState ->
                    div {
                        classes = setOf("ui-empty-state")
                        h4 { +textFrom(emptyState.title) }
                        emptyState.description?.let { description ->
                            p { +textFrom(description) }
                        }
                    }
                }
            } else {
                items.forEachIndexed { index, _ ->
                    val itemScope = scope.push(index)
                    div {
                        classes = setOf("ui-collection-item")
                        attributes["data-ui-index"] = index.toString()
                        renderCollectionItemHeader(node, state, itemScope)
                        renderNodeContent(spec, state, node.itemTemplate, messages, settings, itemScope)
                    }
                }
            }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderCollectionItemHeader(
    node: io.amichne.konditional.uispec.UiCollection,
    state: S,
    scope: PointerScope,
) {
    val label = node.itemKeyPointer?.let { pointer ->
        val resolved = resolvePointer(pointer, scope)
        state.read(resolved).asComparableString()
    }
    if (label != null) {
        div {
            classes = setOf("ui-collection-item-header")
            h4 { +label }
        }
    }
}

private fun <S : UiValueProvider> FlowContent.renderFieldNode(
    state: S,
    node: UiField,
    settings: UiRenderSettings,
    scope: PointerScope,
) {
    if (node.isVisible(state, scope)) {
        val resolvedTarget = resolvePointer(node.target, scope)
        val value = state.read(resolvedTarget)
        val enabled = node.isEnabled(state, scope) && !node.readOnly
        div {
            classes = setOf("ui-field")
            attributes["data-ui-node"] = node.id.value
            attributes["data-ui-pointer"] = resolvedTarget.value
            attributes["data-ui-control"] = node.control.name.lowercase()
            attributes["data-ui-kind"] = node.valueKind.name.lowercase()
            node.meta.title?.let { title ->
                label {
                    classes = setOf("ui-field-label")
                    +textFrom(title)
                }
            }
            node.meta.description?.let { description ->
                p {
                    classes = setOf("ui-field-description")
                    +textFrom(description)
                }
            }
            renderFieldControl(node, value, resolvedTarget, enabled, settings)
            node.meta.helpText?.let { helpText ->
                p {
                    classes = setOf("ui-field-help")
                    +textFrom(helpText)
                }
            }
        }
    }
}

private fun FlowContent.renderFieldControl(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) =
    when (node.control) {
        UiControlType.TOGGLE -> renderToggle(node, value, target, enabled, settings)
        UiControlType.TEXT -> renderTextInput(node, value, target, enabled, settings)
        UiControlType.TEXTAREA -> renderTextArea(node, value, target, enabled, settings)
        UiControlType.NUMBER -> renderNumberInput(node, value, target, enabled, settings)
        UiControlType.PERCENT -> renderPercentInput(node, value, target, enabled, settings)
        UiControlType.SELECT -> renderSelectInput(node, value, target, enabled, settings)
        UiControlType.MULTISELECT -> renderMultiSelectInput(node, value, target, enabled, settings)
        UiControlType.KEY_VALUE -> renderKeyValueInput(node, value, target, enabled, settings)
        UiControlType.JSON -> renderJsonInput(node, value, target, enabled, settings)
        UiControlType.SEMVER -> renderTextInput(node, value, target, enabled, settings)
        UiControlType.SEMVER_RANGE -> renderTextInput(node, value, target, enabled, settings)
    }

private fun FlowContent.renderToggle(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    checkBoxInput {
        name = target.value
        checked = value.asBoolean()
        disabled = !enabled
        applyFieldAttributes(node, target, settings)
    }
}

private fun FlowContent.renderTextInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    input(type = InputType.text) {
        name = target.value
        this.value = value.asInputString()
        disabled = !enabled
        node.placeholder?.let { placeholder = textFrom(it) }
        applyFieldAttributes(node, target, settings)
    }
}

private fun FlowContent.renderTextArea(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    textArea {
        name = target.value
        disabled = !enabled
        node.placeholder?.let { placeholder = textFrom(it) }
        node.inputHints.rows?.let { rows = it.toString() }
        applyFieldAttributes(node, target, settings)
        +value.asInputString()
    }
}

private fun FlowContent.renderNumberInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    input(type = InputType.number) {
        name = target.value
        this.value = value.asInputString()
        disabled = !enabled
        node.inputHints.min?.let { min = it.toString() }
        node.inputHints.max?.let { max = it.toString() }
        node.inputHints.step?.let { step = it.toString() }
        node.placeholder?.let { placeholder = textFrom(it) }
        applyFieldAttributes(node, target, settings)
    }
}

private fun FlowContent.renderPercentInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    input(type = InputType.number) {
        name = target.value
        this.value = value.asInputString()
        disabled = !enabled
        min = (node.inputHints.min ?: 0.0).toString()
        max = (node.inputHints.max ?: 100.0).toString()
        step = (node.inputHints.step ?: 1.0).toString()
        node.placeholder?.let { placeholder = textFrom(it) }
        applyFieldAttributes(node, target, settings)
    }
}

private fun FlowContent.renderSelectInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
        val selectedValue = value.asComparableString()
    select {
        name = target.value
        disabled = !enabled
        applyFieldAttributes(node, target, settings)
        node.options.forEach { optionItem ->
            option {
                this.value = optionItem.value
                selected = optionItem.value == selectedValue
                +textFrom(optionItem.label)
            }
        }
    }
}

private fun FlowContent.renderMultiSelectInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    val selectedValues = value.asStringList()
    select {
        name = target.value
        multiple = true
        disabled = !enabled
        applyFieldAttributes(node, target, settings)
        node.options.forEach { optionItem ->
            option {
                this.value = optionItem.value
                selected = selectedValues.contains(optionItem.value)
                +textFrom(optionItem.label)
            }
        }
    }
}

private fun FlowContent.renderKeyValueInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    textArea {
        name = target.value
        disabled = !enabled
        node.placeholder?.let { placeholder = textFrom(it) }
        node.inputHints.rows?.let { rows = it.toString() }
        applyFieldAttributes(node, target, settings)
        +value.asInputString()
    }
}

private fun FlowContent.renderJsonInput(
    node: UiField,
    value: UiValue?,
    target: JsonPointer,
    enabled: Boolean,
    settings: UiRenderSettings,
) {
    textArea {
        name = target.value
        disabled = !enabled
        node.placeholder?.let { placeholder = textFrom(it) }
        node.inputHints.rows?.let { rows = it.toString() }
        applyFieldAttributes(node, target, settings)
        +value.asInputString()
    }
}

private fun Tag.applyFieldAttributes(node: UiField, target: JsonPointer, settings: UiRenderSettings) {
    attributes["data-ui-pointer"] = target.value
    attributes["data-ui-control"] = node.control.name.lowercase()
    attributes["data-ui-kind"] = node.valueKind.name.lowercase()
    attributes["data-ui-required"] = node.required.toString()
    attributes["data-ui-readonly"] = node.readOnly.toString()
    attributes["hx-patch"] = settings.paths.patch
    attributes["hx-trigger"] = "change"
    attributes["hx-target"] = "#${settings.rootId}"
    attributes["hx-swap"] = "innerHTML"
    attributes["hx-ext"] = "json-enc"
    attributes["hx-headers"] = "{\"Content-Type\":\"application/json\"}"
    attributes["hx-vals"] = htmxVals(target.value)
}

private fun Tag.applyLayout(layout: io.amichne.konditional.uispec.UiLayout) {
    attributes["data-ui-direction"] = layout.direction.name.lowercase()
    attributes["data-ui-spacing"] = layout.spacing.name.lowercase()
    attributes["data-ui-density"] = layout.density.name.lowercase()
}

private fun FlowContent.renderMeta(node: UiNode) {
    node.meta.title?.let { title ->
        h2 { +textFrom(title) }
    }
    node.meta.description?.let { description ->
        p { +textFrom(description) }
    }
}

private fun FlowContent.renderTabMeta(tab: UiTab) {
    tab.meta.title?.let { title ->
        h3 { +textFrom(title) }
    }
    tab.meta.description?.let { description ->
        p { +textFrom(description) }
    }
}

private data class PointerScope(val indices: List<Int>) {
    companion object {
        val empty: PointerScope = PointerScope(emptyList())
    }

    fun push(index: Int): PointerScope = PointerScope(indices + index)
}

private fun resolvePointer(pointer: JsonPointer, scope: PointerScope): JsonPointer =
    pointer.value.split("/").let { parts ->
        val resolved = parts.fold(ResolutionState(scope.indices, mutableListOf())) { state, part ->
            if (part == "*" && state.remaining.isNotEmpty()) {
                state.consume()
            } else {
                state.keep(part)
            }
        }
        JsonPointer(resolved.tokens.joinToString("/"))
    }

private data class ResolutionState(
    val remaining: List<Int>,
    val tokens: MutableList<String>,
) {
    fun consume(): ResolutionState =
        if (remaining.isNotEmpty()) {
            tokens.add(remaining.first().toString())
            ResolutionState(remaining.drop(1), tokens)
        } else {
            this
        }

    fun keep(part: String): ResolutionState =
        ResolutionState(remaining, tokens.apply { add(part) })
}

private fun UiValue?.asArrayValues(): List<UiValue> =
    when (this) {
        is UiArray -> value
        else -> emptyList()
    }

private fun UiValue?.asStringList(): List<String> =
    when (this) {
        is UiArray -> value.mapNotNull { it.asComparableString() }
        else -> emptyList()
    }

private fun UiValue?.asBoolean(): Boolean =
    (this as? UiBoolean)?.value ?: false

private fun UiValue?.asComparableString(): String? =
    when (this) {
        null -> null
        UiNull -> null
        is UiEnum -> value
        is UiString -> value
        is UiInt -> value.toString()
        is UiDouble -> value.toString()
        is UiBoolean -> value.toString()
        is io.amichne.konditional.uispec.UiJson -> value
        else -> null
    }

private fun UiValue?.asInputString(): String =
    when (this) {
        null -> ""
        UiNull -> ""
        is UiString -> value
        is UiEnum -> value
        is UiBoolean -> value.toString()
        is UiInt -> value.toString()
        is UiDouble -> value.toString()
        is io.amichne.konditional.uispec.UiJson -> value
        is UiArray -> value.joinToString(prefix = "[", postfix = "]") { it.asInputString() }
        is UiObject -> value.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\": ${it.value.asInputString()}"
        }
        is UiMap -> value.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\": ${it.value.asInputString()}"
        }
    }

private fun UiNode.isVisible(state: UiValueProvider, scope: PointerScope): Boolean =
    visibility?.matches(state, scope) ?: true

private fun UiNode.isEnabled(state: UiValueProvider, scope: PointerScope): Boolean =
    enabled?.matches(state, scope) ?: true

private fun UiTab.isVisible(state: UiValueProvider, scope: PointerScope): Boolean =
    visibility?.matches(state, scope) ?: true

private fun UiConditionGroup.matches(state: UiValueProvider, scope: PointerScope): Boolean =
    allOf.all { it.matches(state, scope) } &&
        (anyOf.isEmpty() || anyOf.any { it.matches(state, scope) }) &&
        noneOf.none { it.matches(state, scope) }

private fun UiCondition.matches(state: UiValueProvider, scope: PointerScope): Boolean =
    resolvePointer(pointer, scope).let { resolved ->
        val left = state.read(resolved)
        val leftValue = left.asComparableString()
        val rightValue = value.asComparableString()
        val listValue = (value as? UiArray)?.value?.mapNotNull { it.asComparableString() }.orEmpty()
        when (operator) {
            UiConditionOperator.EXISTS -> left != null && left != UiNull
            UiConditionOperator.EQUALS -> leftValue == rightValue
            UiConditionOperator.NOT_EQUALS -> leftValue != rightValue
            UiConditionOperator.IN -> leftValue != null && listValue.contains(leftValue)
            UiConditionOperator.NOT_IN -> leftValue != null && !listValue.contains(leftValue)
            UiConditionOperator.MATCHES -> leftValue != null && rightValue != null &&
                runCatching { Regex(rightValue).containsMatchIn(leftValue) }.getOrDefault(false)
        }
    }

private fun findNode(root: UiNode, targetId: UiNodeId): UiNode? =
    if (root.id == targetId) {
        root
    } else {
        root.children().firstNotNullOfOrNull { findNode(it, targetId) }
    }

private fun UiNode.children(): List<UiNode> =
    when (this) {
        is UiPage -> children
        is UiSection -> children
        is UiGroup -> children
        is UiTabs -> tabs.flatMap { it.children }
        is io.amichne.konditional.uispec.UiCollection -> listOf(itemTemplate)
        is UiField -> emptyList()
    }

private fun textFrom(text: UiText?): String =
    when (text) {
        null -> ""
        is UiTextLiteral -> text.value
        is UiTextKey -> text.fallback ?: text.key
    }

private fun htmxVals(pointer: String): String =
    "js:{patch:[{op:'replace', path:'${escapeJs(pointer)}', value: window.konditionalUiPatchValue(this)}]}"

private fun escapeJs(value: String): String =
    value.replace("\\", "\\\\").replace("'", "\\'")
