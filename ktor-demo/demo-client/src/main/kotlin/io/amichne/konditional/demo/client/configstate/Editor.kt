package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.JSON
import io.amichne.konditional.demo.client.configstate.Fields.axesMapField
import io.amichne.konditional.demo.client.configstate.Fields.enumMultiSelectField
import io.amichne.konditional.demo.client.configstate.Fields.flagValueField
import io.amichne.konditional.demo.client.configstate.Fields.multilineStringListField
import io.amichne.konditional.demo.client.configstate.Fields.numberField
import io.amichne.konditional.demo.client.configstate.Fields.textField
import io.amichne.konditional.demo.client.configstate.Fields.textareaField
import io.amichne.konditional.demo.client.configstate.Fields.toggleField
import io.amichne.konditional.demo.client.configstate.Fields.versionRangeField
import io.amichne.konditional.demo.client.configstate.HtmlLayout.fieldSectionTitle
import io.amichne.konditional.demo.client.configstate.HtmlLayout.div
import io.amichne.konditional.demo.client.configstate.Json.dynamicStringArray
import io.amichne.konditional.demo.client.configstate.Json.newEmptyRuleBasedOnFlag
import io.amichne.konditional.demo.client.configstate.Json.stableJson
import io.amichne.konditional.demo.client.configstate.Json.stableString
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

object Editor {

    internal fun renderEditor(
        layout: Layout,
        state: LoadedState,
    ) {
        layout.editor.innerHTML = ""

        val flags = state.draftSnapshot.flags.unsafeCast<Array<dynamic>>()
        val selectedIndex = state.selectedFlagIndex?.takeIf { it in flags.indices } ?: return
        val flag = flags[selectedIndex]

        val header = div("editor-header")
        val title = div("editor-title")
        title.textContent = stableString(flag.key)
        header.appendChild(title)
        layout.editor.appendChild(header)

        val tabs = div("tabs")
        layout.editor.appendChild(tabs)

        val tabButtons = div("tab-buttons")
        val tabContent = div("tab-content")
        tabs.appendChild(tabButtons)
        tabs.appendChild(tabContent)

        fun activateTab(name: String) {
            for (i in 0 until tabButtons.children.length) {
                val child = tabButtons.children.item(i) as? HTMLElement ?: continue
                child.classList.toggle("active", child.getAttribute("data-tab") == name)
            }
            for (i in 0 until tabContent.children.length) {
                val child = tabContent.children.item(i) as? HTMLElement ?: continue
                child.classList.toggle("active", child.getAttribute("data-tab") == name)
            }
        }

        fun tabButton(
            name: String,
            label: String,
        ): HTMLButtonElement {
            val btn =
                (document.createElement("button") as HTMLButtonElement).also {
                    it.type = "button"
                    it.className = "tab-btn"
                    it.setAttribute("data-tab", name)
                    it.textContent = label
                    it.addEventListener("click", { _ -> activateTab(name) })
                }
            tabButtons.appendChild(btn)
            return btn
        }

        fun tabPanel(name: String): HTMLDivElement =
            (document.createElement("div") as HTMLDivElement).also {
                it.className = "tab-panel"
                it.setAttribute("data-tab", name)
                tabContent.appendChild(it)
            }

        tabButton("flag", "Flag")
        tabButton("rules", "Rules")
        tabButton("raw", "Raw")

        renderFlagPanel(layout, tabPanel("flag"), selectedIndex, flag)
        renderRulesPanel(layout, tabPanel("rules"), selectedIndex, flag)
        renderRawPanel(layout, tabPanel("raw"), selectedIndex, flag)

        activateTab("flag")
    }

    private fun renderFlagPanel(
        layout: Layout,
        panel: HTMLDivElement,
        flagIndex: Int,
        flag: dynamic,
    ) {
        val supported = Client.supportedValues() ?: return

        panel.appendChild(fieldSectionTitle("Flag settings"))
        panel.appendChild(
            toggleField(
                typeName = "FLAG_ACTIVE",
                value = (flag.isActive as? Boolean) ?: true,
                supported = supported,
                onChange = { newValue -> Client.updateAtPointer(layout, "/flags/$flagIndex/isActive", newValue) },
            ),
        )

        panel.appendChild(
            textField(
                typeName = "SALT",
                value = stableString(flag.salt),
                supported = supported,
                onChange = { newValue -> Client.updateAtPointer(layout, "/flags/$flagIndex/salt", newValue) },
            ),
        )

        panel.appendChild(
            multilineStringListField(
                typeName = "RAMP_UP_ALLOWLIST",
                values = dynamicStringArray(flag.rampUpAllowlist),
                supported = supported,
                onChange = { newValues ->
                    Client.updateAtPointer(
                        layout,
                        "/flags/$flagIndex/rampUpAllowlist",
                        newValues
                    )
                },
            ),
        )

        panel.appendChild(fieldSectionTitle("Default value"))
        panel.appendChild(
            flagValueField(
                value = flag.defaultValue,
                supported = supported,
                onChange = { newValue -> Client.updateAtPointer(layout, "/flags/$flagIndex/defaultValue", newValue) },
            ),
        )
    }

    private fun renderRulesPanel(
        layout: Layout,
        panel: HTMLDivElement,
        flagIndex: Int,
        flag: dynamic,
    ) {
        val supported = Client.supportedValues() ?: return
        val rules = (flag.rules ?: emptyArray<dynamic>()).unsafeCast<Array<dynamic>>()

        val header = div("section-row")
        header.appendChild(fieldSectionTitle("Rules"))
        val addRuleBtn =
            (document.createElement("button") as HTMLButtonElement).also {
                it.type = "button"
                it.className = "btn btn-secondary"
                it.textContent = "Add rule"
                it.addEventListener("click", { _ ->
                    val newRule = newEmptyRuleBasedOnFlag(flag)
                    val updated = rules.toMutableList().apply { add(newRule) }.toTypedArray()
                    Client.updateAtPointer(layout, "/flags/$flagIndex/rules", updated)
                })
            }
        header.appendChild(addRuleBtn)
        panel.appendChild(header)

        if (rules.isEmpty()) {
            panel.appendChild(div("empty").also { it.textContent = "No rules. Add one to override the default." })
            return
        }

        rules.forEachIndexed { ruleIndex, rule ->
            val card = div("rule-card")
            val cardHeader = div("rule-header")
            val ruleTitle = div("rule-title").also { it.textContent = "Rule ${ruleIndex + 1}" }
            val removeBtn =
                (document.createElement("button") as HTMLButtonElement).also {
                    it.type = "button"
                    it.className = "btn btn-danger"
                    it.textContent = "Remove"
                    it.addEventListener("click", { _ ->
                        val updated = rules.toMutableList().apply { removeAt(ruleIndex) }.toTypedArray()
                        Client.updateAtPointer(layout, "/flags/$flagIndex/rules", updated)
                    })
                }
            cardHeader.appendChild(ruleTitle)
            cardHeader.appendChild(removeBtn)
            card.appendChild(cardHeader)

            card.appendChild(
                numberField(
                    typeName = "RAMP_UP_PERCENT",
                    value = (rule.rampUp as? Number)?.toDouble() ?: 100.0,
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/rampUp",
                            newValue
                        )
                    },
                ),
            )

            card.appendChild(
                multilineStringListField(
                    typeName = "RAMP_UP_ALLOWLIST",
                    values = dynamicStringArray(rule.rampUpAllowlist),
                    supported = supported,
                    onChange = { newValues ->
                        Client.updateAtPointer(layout, "/flags/$flagIndex/rules/$ruleIndex/rampUpAllowlist", newValues)
                    },
                ),
            )

            card.appendChild(
                textareaField(
                    typeName = "RULE_NOTE",
                    value = stableString(rule.note ?: ""),
                    supported = supported,
                    onChange = { newValue ->
                        val trimmed = newValue.trim()
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/note",
                            trimmed.takeIf { it.isNotBlank() },
                        )
                    },
                ),
            )

            card.appendChild(
                enumMultiSelectField(
                    typeName = "LOCALES",
                    selected = dynamicStringArray(rule.locales),
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/locales",
                            newValue
                        )
                    },
                ),
            )

            card.appendChild(
                enumMultiSelectField(
                    typeName = "PLATFORMS",
                    selected = dynamicStringArray(rule.platforms),
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/platforms",
                            newValue
                        )
                    },
                ),
            )

            card.appendChild(
                versionRangeField(
                    typeName = "VERSION_RANGE",
                    value = rule.versionRange,
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/versionRange",
                            newValue
                        )
                    },
                ),
            )

            card.appendChild(
                axesMapField(
                    typeName = "AXES_MAP",
                    value = rule.axes,
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/axes",
                            newValue
                        )
                    },
                ),
            )

            card.appendChild(fieldSectionTitle("Rule value"))
            card.appendChild(
                flagValueField(
                    value = rule.value,
                    supported = supported,
                    onChange = { newValue ->
                        Client.updateAtPointer(
                            layout,
                            "/flags/$flagIndex/rules/$ruleIndex/value",
                            newValue
                        )
                    },
                ),
            )

            panel.appendChild(card)
        }
    }

    private fun renderRawPanel(
        layout: Layout,
        panel: HTMLDivElement,
        flagIndex: Int,
        flag: dynamic,
    ) {
        panel.appendChild(fieldSectionTitle("Selected flag JSON"))

        val textarea =
            (document.createElement("textarea") as HTMLTextAreaElement).also {
                it.className = "mono"
                it.value = stableJson(flag)
            }
        panel.appendChild(textarea)

        val row = div("button-row")
        val applyBtn =
            (document.createElement("button") as HTMLButtonElement).also {
                it.type = "button"
                it.className = "btn btn-primary"
                it.textContent = "Apply to flag"
                it.addEventListener("click", { _ ->
                    try {
                        val parsed = JSON.parse(textarea.value)
                        Client.updateAtPointer(layout, "/flags/$flagIndex", parsed)
                        Client.setStatusReady(layout, "Applied raw flag JSON.")
                    } catch (e: dynamic) {
                        val msg = js("e.message || e.toString() || 'Unknown error'") as String
                        Client.setStatusError(layout, "Invalid JSON: $msg")
                    }
                })
            }

        val copyBtn =
            (document.createElement("button") as HTMLButtonElement).also {
                it.type = "button"
                it.className = "btn btn-secondary"
                it.textContent = "Copy"
                it.addEventListener("click", { _ ->
                    window.navigator.asDynamic().clipboard?.writeText(textarea.value)
                })
            }

        row.appendChild(applyBtn)
        row.appendChild(copyBtn)
        panel.appendChild(row)
    }
}
