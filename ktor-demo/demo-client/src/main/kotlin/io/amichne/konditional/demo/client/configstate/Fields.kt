package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.configstate.HtmlLayout.fieldHelp
import io.amichne.konditional.demo.client.configstate.HtmlLayout.fieldLabel
import io.amichne.konditional.demo.client.configstate.HtmlLayout.button
import io.amichne.konditional.demo.client.configstate.HtmlLayout.div
import io.amichne.konditional.demo.client.configstate.Json.readVersion
import io.amichne.konditional.demo.client.configstate.Json.asElements
import io.amichne.konditional.demo.client.configstate.Json.stableJson
import io.amichne.konditional.demo.client.configstate.Json.stableString
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.RegExp
import kotlin.js.json

object Fields {
    internal fun toggleField(
        typeName: String,
        value: Boolean,
        supported: dynamic,
        onChange: (Boolean) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val row = div("button-row")
        val input =
            (document.createElement("input") as HTMLInputElement).also {
                it.type = "checkbox"
                it.checked = value
                it.addEventListener("change", { event ->
                    val checkbox = event.target as HTMLInputElement
                    onChange(checkbox.checked)
                })
            }
        val wrapper = div("checkbox")
        wrapper.appendChild(input)
        wrapper.appendChild(div().also { it.textContent = "Enabled" })
        row.appendChild(wrapper)
        field.appendChild(row)
        return field
    }

    internal fun textField(
        typeName: String,
        value: String,
        supported: dynamic,
        onChange: (String) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints
        val pattern = descriptor.pattern as String?
        val minLength = (descriptor.minLength as? Number)?.toInt()
        val maxLength = (descriptor.maxLength as? Number)?.toInt()

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val input =
            (document.createElement("input") as HTMLInputElement).also {
                it.type = "text"
                it.className = "input"
                it.value = value

                val placeholder = uiHints.placeholder as String?
                if (!placeholder.isNullOrBlank()) {
                    it.placeholder = placeholder
                }

                pattern?.let { regex -> setPatternIfSupported(it, regex) }
                minLength?.let { min -> it.minLength = min }
                maxLength?.let { max -> it.maxLength = max }
                it.addEventListener("input", { event ->
                    val text = (event.target as HTMLInputElement).value
                    onChange(text)
                })
            }
        field.appendChild(input)
        return field
    }

    internal fun textareaField(
        typeName: String,
        value: String,
        supported: dynamic,
        onChange: (String) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints
        val maxLength = (descriptor.maxLength as? Number)?.toInt()

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val input =
            (document.createElement("textarea") as HTMLTextAreaElement).also {
                it.className = "mono"
                it.value = value

                val placeholder = uiHints.placeholder as String?
                if (!placeholder.isNullOrBlank()) {
                    it.placeholder = placeholder
                }

                maxLength?.let { max -> it.maxLength = max }
                it.addEventListener("input", { event ->
                    val text = (event.target as HTMLTextAreaElement).value
                    onChange(text)
                })
            }
        field.appendChild(input)
        return field
    }

    internal fun numberField(
        typeName: String,
        value: Double,
        supported: dynamic,
        onChange: (Double) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints
        val min = (descriptor.min as? Number)?.toDouble()
        val max = (descriptor.max as? Number)?.toDouble()
        val step = (descriptor.step as? Number)?.toDouble()
        val unit = descriptor.unit as String?

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val input =
            (document.createElement("input") as HTMLInputElement).also {
                it.type = "number"
                it.className = "input"
                it.value = value.toString()
                min?.let { m -> it.min = m.toString() }
                max?.let { m -> it.max = m.toString() }
                step?.let { s -> it.step = s.toString() }
                it.addEventListener("input", { event ->
                    val parsed = (event.target as HTMLInputElement).value.toDoubleOrNull()
                    if (parsed != null) {
                        onChange(parsed)
                    }
                })
            }
        field.appendChild(input)
        unit?.let { field.appendChild(div("field-help").also { it.textContent = "Unit: $unit" }) }
        return field
    }

    internal fun multilineStringListField(
        typeName: String,
        values: List<String>,
        supported: dynamic,
        onChange: (Array<String>) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val input =
            (document.createElement("textarea") as HTMLTextAreaElement).also {
                it.className = "mono"
                it.value = values.joinToString(separator = "\n")
                it.addEventListener("input", { event ->
                    val raw = (event.target as HTMLTextAreaElement).value
                    val parsed =
                        raw
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .toTypedArray()
                    onChange(parsed)
                })
            }
        field.appendChild(input)
        return field
    }

    internal fun enumMultiSelectField(
        typeName: String,
        selected: List<String>,
        supported: dynamic,
        onChange: (Array<String>) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints
        val options = (descriptor.options ?: emptyArray<dynamic>()).unsafeCast<Array<dynamic>>()
        val selectedSet = selected.toSet()

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val controls = div("field-controls")
        val search =
            (document.createElement("input") as HTMLInputElement).also {
                it.type = "search"
                it.className = "search search-inline"
                it.placeholder = "Search…"
            }
        val counts = div("field-count")
        val allBtn = button("btn btn-secondary btn-sm", "All")
        val noneBtn = button("btn btn-secondary btn-sm", "None")

        val grid = div("checkbox-grid")
        val showSearch = options.size > 12
        if (showSearch) {
            controls.appendChild(search)
        }
        controls.appendChild(counts)
        controls.appendChild(allBtn)
        controls.appendChild(noneBtn)
        field.appendChild(controls)

        fun computeChecked(): Array<String> =
            (0 until grid.children.length)
                .mapNotNull { i ->
                    val row = grid.children.item(i) as? HTMLElement ?: return@mapNotNull null
                    val input = row.querySelector("input") as? HTMLInputElement ?: return@mapNotNull null
                    val v = row.getAttribute("data-value") ?: return@mapNotNull null
                    v.takeIf { input.checked }
                }.distinct()
                .toTypedArray()

        fun updateCounts() {
            val selectedCount = computeChecked().size
            counts.textContent = "$selectedCount selected"
        }

        fun applyFilter(query: String) {
            val q = query.trim().lowercase()
            for (i in 0 until grid.children.length) {
                val row = grid.children.item(i) as? HTMLElement ?: continue
                val haystack = row.getAttribute("data-search") ?: ""
                val visible = q.isBlank() || haystack.contains(q)
                row.style.display = if (visible) "" else "none"
            }
        }

        options.forEach { option ->
            val value = stableString(option.value)
            val labelText = stableString(option.label)
            val checkbox =
                (document.createElement("input") as HTMLInputElement).also {
                    it.type = "checkbox"
                    it.checked = value in selectedSet
                    it.addEventListener("change", { _ ->
                        onChange(computeChecked())
                        updateCounts()
                    })
                }
            val row =
                div("checkbox").also {
                    it.setAttribute("data-value", value)
                    it.setAttribute("data-search", "$value $labelText".lowercase())
                    it.appendChild(checkbox)
                    it.appendChild(div().also { el -> el.textContent = labelText })
                }
            grid.appendChild(row)
        }
        field.appendChild(grid)
        updateCounts()

        if (showSearch) {
            search.addEventListener("input", { _ ->
                applyFilter(search.value)
            })
        }

        allBtn.addEventListener("click", { _ ->
            for (i in 0 until grid.children.length) {
                val row = grid.children.item(i) as? HTMLElement ?: continue
                val input = row.querySelector("input") as? HTMLInputElement ?: continue
                if (row.style.display != "none") {
                    input.checked = true
                }
            }
            onChange(computeChecked())
            updateCounts()
        })

        noneBtn.addEventListener("click", { _ ->
            for (i in 0 until grid.children.length) {
                val row = grid.children.item(i) as? HTMLElement ?: continue
                val input = row.querySelector("input") as? HTMLInputElement ?: continue
                if (row.style.display != "none") {
                    input.checked = false
                }
            }
            onChange(computeChecked())
            updateCounts()
        })
        return field
    }

    internal fun flagValueField(
        value: dynamic,
        supported: dynamic,
        onChange: (dynamic) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType["FLAG_VALUE"]
        val uiHints = descriptor.uiHints

        val field = fieldContainer(uiHints, "FLAG_VALUE")
        field.appendChild(fieldLabel(uiHints, fallback = "FLAG_VALUE"))
        field.appendChild(fieldHelp(uiHints))

        val typeSelect =
            (document.createElement("select") as HTMLSelectElement).also {
                it.className = "select"
            }
        listOf("BOOLEAN", "STRING", "INT", "DOUBLE", "ENUM", "DATA_CLASS").forEach { type ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.value = type
            opt.textContent = type
            typeSelect.appendChild(opt)
        }

        val currentType = stableString(value.type ?: "BOOLEAN")
        typeSelect.value = currentType

        val editorBody = div()

        fun setValue(newValue: dynamic) {
            onChange(newValue)
        }

        fun renderEditorBody(type: String) {
            editorBody.innerHTML = ""
            when (type) {
                "BOOLEAN" -> {
                    val checkbox =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "checkbox"
                            it.checked = (value.value as? Boolean) ?: false
                            it.addEventListener("change", { event ->
                                val v = (event.target as HTMLInputElement).checked
                                setValue(json("type" to "BOOLEAN", "value" to v))
                            })
                        }
                    val row = div("checkbox")
                    row.appendChild(checkbox)
                    row.appendChild(div().also { it.textContent = "true/false" })
                    editorBody.appendChild(row)
                }

                "STRING" -> {
                    val input =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "text"
                            it.className = "input"
                            it.value = stableString(value.value ?: "")
                            it.addEventListener("input", { event ->
                                val v = (event.target as HTMLInputElement).value
                                setValue(json("type" to "STRING", "value" to v))
                            })
                        }
                    editorBody.appendChild(input)
                }

                "INT" -> {
                    val input =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "number"
                            it.step = "1"
                            it.className = "input"
                            it.value = ((value.value as? Number)?.toInt() ?: 0).toString()
                            it.addEventListener("input", { event ->
                                val v = (event.target as HTMLInputElement).value.toIntOrNull() ?: 0
                                setValue(json("type" to "INT", "value" to v))
                            })
                        }
                    editorBody.appendChild(input)
                }

                "DOUBLE" -> {
                    val input =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "number"
                            it.step = "0.1"
                            it.className = "input"
                            it.value = ((value.value as? Number)?.toDouble() ?: 0.0).toString()
                            it.addEventListener("input", { event ->
                                val v = (event.target as HTMLInputElement).value.toDoubleOrNull() ?: 0.0
                                setValue(json("type" to "DOUBLE", "value" to v))
                            })
                        }
                    editorBody.appendChild(input)
                }

                "ENUM" -> {
                    val enumClassInput =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "text"
                            it.className = "input"
                            it.placeholder = "enumClassName (fully qualified)"
                            it.value = stableString(value.enumClassName ?: "")
                        }
                    val enumValueInput =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "text"
                            it.className = "input"
                            it.placeholder = "enum value (name)"
                            it.value = stableString(value.value ?: "")
                        }

                    fun commit() {
                        setValue(
                            json(
                                "type" to "ENUM",
                                "enumClassName" to enumClassInput.value,
                                "value" to enumValueInput.value,
                            ),
                        )
                    }
                    enumClassInput.addEventListener("input", { _ -> commit() })
                    enumValueInput.addEventListener("input", { _ -> commit() })
                    editorBody.appendChild(enumClassInput)
                    editorBody.appendChild(enumValueInput)
                }

                "DATA_CLASS" -> {
                    val classNameInput =
                        (document.createElement("input") as HTMLInputElement).also {
                            it.type = "text"
                            it.className = "input"
                            it.placeholder = "dataClassName (fully qualified)"
                            it.value = stableString(value.dataClassName ?: "")
                        }
                    val jsonInput =
                        (document.createElement("textarea") as HTMLTextAreaElement).also {
                            it.className = "mono"
                            it.placeholder = """{"field": 1, "nested": {"ok": true}}"""
                            it.value = stableJson(value.value ?: json())
                        }

                    fun commit() {
                        try {
                            val parsed = io.amichne.konditional.demo.client.JSON.parse(jsonInput.value)
                            setValue(
                                json(
                                    "type" to "DATA_CLASS",
                                    "dataClassName" to classNameInput.value,
                                    "value" to parsed,
                                ),
                            )
                        } catch (_: dynamic) {
                            Unit
                        }
                    }

                    classNameInput.addEventListener("input", { _ -> commit() })
                    jsonInput.addEventListener("input", { _ -> commit() })
                    editorBody.appendChild(classNameInput)
                    editorBody.appendChild(jsonInput)
                }
            }
        }

        typeSelect.addEventListener("change", { event ->
            val selectedType = (event.target as HTMLSelectElement).value
            val newValue =
                when (selectedType) {
                    "BOOLEAN" -> json("type" to "BOOLEAN", "value" to false)
                    "STRING" -> json("type" to "STRING", "value" to "")
                    "INT" -> json("type" to "INT", "value" to 0)
                    "DOUBLE" -> json("type" to "DOUBLE", "value" to 0.0)
                    "ENUM" -> json("type" to "ENUM", "value" to "", "enumClassName" to "")
                    "DATA_CLASS" -> json("type" to "DATA_CLASS", "value" to json(), "dataClassName" to "")
                    else -> json("type" to "BOOLEAN", "value" to false)
                }
            onChange(newValue)
            renderEditorBody(selectedType)
        })

        field.appendChild(typeSelect)
        field.appendChild(editorBody)
        renderEditorBody(currentType)
        return field
    }

    internal fun versionRangeField(
        typeName: String,
        value: dynamic,
        supported: dynamic,
        onChange: (dynamic) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val typeSelect = (document.createElement("select") as HTMLSelectElement).also { it.className = "select" }
        listOf("UNBOUNDED", "MIN_BOUND", "MAX_BOUND", "MIN_AND_MAX_BOUND").forEach { type ->
            val opt = document.createElement("option") as HTMLOptionElement
            opt.value = type
            opt.textContent = type
            typeSelect.appendChild(opt)
        }

        val currentType = stableString(value?.type ?: "UNBOUNDED")
        typeSelect.value = currentType

        val editorBody = div()

        fun versionInputs(prefix: String, existing: dynamic): HTMLElement {
            val wrap = div()
            val major =
                (document.createElement("input") as HTMLInputElement).also {
                    it.type = "number"
                    it.step = "1"
                    it.className = "input"
                    it.placeholder = "$prefix major"
                    it.value = ((existing?.major as? Number)?.toInt() ?: 0).toString()
                }
            val minor =
                (document.createElement("input") as HTMLInputElement).also {
                    it.type = "number"
                    it.step = "1"
                    it.className = "input"
                    it.placeholder = "$prefix minor"
                    it.value = ((existing?.minor as? Number)?.toInt() ?: 0).toString()
                }
            val patch =
                (document.createElement("input") as HTMLInputElement).also {
                    it.type = "number"
                    it.step = "1"
                    it.className = "input"
                    it.placeholder = "$prefix patch"
                    it.value = ((existing?.patch as? Number)?.toInt() ?: 0).toString()
                }
            wrap.appendChild(major)
            wrap.appendChild(minor)
            wrap.appendChild(patch)
            wrap.setAttribute("data-version", prefix)
            return wrap
        }

        fun commit(type: String) {
            val newValue =
                when (type) {
                    "UNBOUNDED" -> null
                    "MIN_BOUND" -> {
                        val minWrap = editorBody.querySelector("[data-version='min']") as? HTMLElement
                        val v = readVersion(minWrap)
                        json("type" to "MIN_BOUND", "min" to v)
                    }
                    "MAX_BOUND" -> {
                        val maxWrap = editorBody.querySelector("[data-version='max']") as? HTMLElement
                        val v = readVersion(maxWrap)
                        json("type" to "MAX_BOUND", "max" to v)
                    }
                    "MIN_AND_MAX_BOUND" -> {
                        val minWrap = editorBody.querySelector("[data-version='min']") as? HTMLElement
                        val maxWrap = editorBody.querySelector("[data-version='max']") as? HTMLElement
                        val minV = readVersion(minWrap)
                        val maxV = readVersion(maxWrap)
                        json("type" to "MIN_AND_MAX_BOUND", "min" to minV, "max" to maxV)
                    }
                    else -> null
                }
            onChange(newValue)
        }

        fun renderEditor(type: String) {
            editorBody.innerHTML = ""
            when (type) {
                "UNBOUNDED" -> editorBody.appendChild(div("field-help").also {
                    it.textContent = "No version restriction."
                })
                "MIN_BOUND" -> editorBody.appendChild(versionInputs("min", value?.min))
                "MAX_BOUND" -> editorBody.appendChild(versionInputs("max", value?.max))
                "MIN_AND_MAX_BOUND" -> {
                    editorBody.appendChild(versionInputs("min", value?.min))
                    editorBody.appendChild(versionInputs("max", value?.max))
                }
            }
            editorBody.querySelectorAll("input").asElements().forEach { input ->
                input.addEventListener("input", { _ -> commit(type) })
            }
        }

        typeSelect.addEventListener("change", { event ->
            val selectedType = (event.target as HTMLSelectElement).value
            renderEditor(selectedType)
            commit(selectedType)
        })

        field.appendChild(typeSelect)
        field.appendChild(editorBody)
        renderEditor(currentType)
        return field
    }

    internal fun axesMapField(
        typeName: String,
        value: dynamic,
        supported: dynamic,
        onChange: (dynamic) -> Unit,
    ): HTMLElement {
        val descriptor = supported.byType[typeName]
        val uiHints = descriptor.uiHints

        val field = fieldContainer(uiHints, typeName)
        field.appendChild(fieldLabel(uiHints, fallback = typeName))
        field.appendChild(fieldHelp(uiHints))

        val current = (value ?: json()).unsafeCast<dynamic>()
        val textarea =
            (document.createElement("textarea") as HTMLTextAreaElement).also {
                it.className = "mono"
                it.value = stableJson(current)
                it.addEventListener("input", { event ->
                    val raw = (event.target as HTMLTextAreaElement).value
                    try {
                        val parsed = io.amichne.konditional.demo.client.JSON.parse(raw)
                        onChange(parsed)
                    } catch (_: dynamic) {
                    }
                })
            }
        field.appendChild(textarea)
        field.appendChild(div("field-help").also { it.textContent = "Format: { \"axisId\": [\"allowedId\"] }" })
        return field
    }

    private fun fieldContainer(
        uiHints: dynamic,
        typeName: String,
    ): HTMLDivElement =
        div("field").also { element ->
            val advanced = (uiHints.advanced as? Boolean) ?: false
            element.setAttribute("data-advanced", advanced.toString())
            element.setAttribute("data-field-type", typeName)
        }

    private fun setPatternIfSupported(
        input: HTMLInputElement,
        pattern: String,
    ) {
        val sanitized = sanitizePattern(pattern)
        if (isPatternSupportedInBrowser(sanitized)) {
            input.pattern = sanitized
        }
    }

    private fun sanitizePattern(pattern: String): String = pattern.replace("-]", "\\-]")

    private fun isPatternSupportedInBrowser(pattern: String): Boolean =
        try {
            RegExp(pattern, "v")
            true
        } catch (_: dynamic) {
            try {
                RegExp(pattern)
                true
            } catch (_: dynamic) {
                false
            }
        }
}
