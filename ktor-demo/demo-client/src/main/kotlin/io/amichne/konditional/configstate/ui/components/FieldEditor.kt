package io.amichne.konditional.configstate.ui.components

import io.amichne.konditional.configstate.ui.json.asDoubleOrNull
import io.amichne.konditional.configstate.ui.json.asStringOrNull
import io.amichne.konditional.configstate.ui.model.BooleanDescriptorDto
import io.amichne.konditional.configstate.ui.model.EnumOptionsDescriptorDto
import io.amichne.konditional.configstate.ui.model.FieldDescriptorDto
import io.amichne.konditional.configstate.ui.model.FieldTypeDto
import io.amichne.konditional.configstate.ui.model.MapConstraintsDescriptorDto
import io.amichne.konditional.configstate.ui.model.NumberRangeDescriptorDto
import io.amichne.konditional.configstate.ui.model.SchemaRefDescriptorDto
import io.amichne.konditional.configstate.ui.model.SemverConstraintsDescriptorDto
import io.amichne.konditional.configstate.ui.model.StringConstraintsDescriptorDto
import io.amichne.konditional.configstate.ui.model.UiControlTypeDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import mui.material.Box
import mui.material.Button
import mui.material.ButtonColor
import mui.material.ButtonVariant
import mui.material.FormControl
import mui.material.FormControlLabel
import mui.material.FormHelperText
import mui.material.InputLabel
import mui.material.MenuItem
import mui.material.Select
import mui.material.Slider
import mui.material.Stack
import mui.material.Switch
import mui.material.TextField
import mui.material.TextFieldProps
import mui.material.Typography
import mui.system.sx
import react.ChildrenBuilder
import react.FC
import react.Props
import react.ReactNode
import react.StateSetter
import react.create
import react.useEffect
import react.useState
import web.cssom.AlignItems
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.FlexGrow
import web.cssom.FontFamily
import web.cssom.pct
import web.cssom.px

external interface FieldEditorProps : Props {
    var fieldType: FieldTypeDto
    var descriptor: FieldDescriptorDto
    var value: JsonElement?
    var onChange: (JsonElement) -> Unit
}

private val jsonPretty: Json = Json { prettyPrint = true }

private fun TextFieldProps.unsafeOnChange(handler: (event: dynamic) -> Unit) {
    asDynamic().onChange = handler
}

val FieldEditor: FC<FieldEditorProps> =
    FC { props ->
        val uiHints = props.descriptor.uiHints

        val (jsonText, setJsonText) = useState("")
        val (jsonError, setJsonError) = useState<String?>(null)

        val (textError, setTextError) = useState<String?>(null)

        val (numberDraft, setNumberDraft) = useState(0.0)

        val (axesRows, setAxesRows) = useState<List<AxisRow>>(emptyList())

        useEffect(props.descriptor, props.value) {
            when (val descriptor = props.descriptor) {
                is NumberRangeDescriptorDto -> {
                    val current = props.value?.asDoubleOrNull() ?: descriptor.min
                    setNumberDraft(current)
                }

                is SchemaRefDescriptorDto -> {
                    val rendered = props.value?.let { jsonPretty.encodeToString(JsonElement.serializer(), it) } ?: "null"
                    setJsonText(rendered)
                    setJsonError(null)
                }

                is MapConstraintsDescriptorDto -> {
                    val initial =
                        (props.value as? JsonObject)
                            ?.mapValues { (_, v) ->
                                (v as? JsonArray)?.mapNotNull { it.asStringOrNull() } ?: emptyList()
                            }.orEmpty()

                    setAxesRows(
                        initial.entries
                            .sortedBy { it.key }
                            .map { (axisId, values) -> AxisRow(axisId = axisId, allowedCsv = values.joinToString(",")) },
                    )
                }

                else -> Unit
            }

            val shouldMirrorJson =
                props.descriptor.uiHints.control == UiControlTypeDto.JSON ||
                    props.descriptor.uiHints.control == UiControlTypeDto.SEMVER_RANGE
            if (shouldMirrorJson && props.descriptor !is SchemaRefDescriptorDto) {
                val rendered = props.value?.let { jsonPretty.encodeToString(JsonElement.serializer(), it) } ?: "null"
                setJsonText(rendered)
                setJsonError(null)
            }
        }

        Stack {
            Typography {
                +(uiHints.label ?: props.fieldType.name)
            }

            uiHints.helpText?.let { help ->
                Typography {
                    +help
                }
            }

            when (val descriptor = props.descriptor) {
                is BooleanDescriptorDto -> {
                    val checked = (props.value as? JsonPrimitive)?.booleanOrNull ?: false
                    FormControlLabel {
                        control =
                            Switch.create {
                                this.checked = checked
                                onChange = { _, nextChecked -> props.onChange(JsonPrimitive(nextChecked)) }
                            }
                        label = ReactNode(uiHints.label ?: props.fieldType.name)
                    }
                }

                is NumberRangeDescriptorDto -> {
                    Stack {
                        Slider {
                            value = numberDraft
                            min = descriptor.min
                            max = descriptor.max
                            step = descriptor.step
                            valueLabelDisplay = "auto"

                            onChange = { _, newValue, _ ->
                                val next = (newValue as? Number)?.toDouble() ?: numberDraft
                                setNumberDraft(next)
                            }

                            onChangeCommitted = { _, newValue ->
                                val next = (newValue as? Number)?.toDouble() ?: numberDraft
                                props.onChange(JsonPrimitive(next))
                            }
                        }

	                        TextField {
	                            label = ReactNode(descriptor.unit?.let { "${uiHints.label ?: props.fieldType.name} ($it)" } ?: (uiHints.label ?: props.fieldType.name))
	                            value = numberDraft.toString()
	                            unsafeOnChange { event: dynamic ->
	                                val nextText = event.target.asDynamic().value as? String
	                                nextText?.toDoubleOrNull()?.let { next ->
	                                    setNumberDraft(next)
	                                    props.onChange(JsonPrimitive(next))
	                                }
	                            }
	                        }
                    }
                }

                is EnumOptionsDescriptorDto -> {
                    when (uiHints.control) {
                        UiControlTypeDto.MULTISELECT -> enumMultiSelect(props, descriptor)
                        else -> enumSelect(props, descriptor)
                    }
                }

                is StringConstraintsDescriptorDto -> {
                    val currentText = props.value?.asStringOrNull() ?: ""

                    TextField {
                        label = ReactNode(uiHints.label ?: props.fieldType.name)
                        placeholder = uiHints.placeholder
                        value = currentText
                        multiline = uiHints.control == UiControlTypeDto.TEXTAREA
                        minRows = if (uiHints.control == UiControlTypeDto.TEXTAREA) 3 else undefined
	                        error = textError != null
	                        helperText = textError?.let(::ReactNode) ?: uiHints.helpText?.let(::ReactNode)

	                        unsafeOnChange { event: dynamic ->
	                            val next = event.target.asDynamic().value as? String ?: ""
	                            setTextError(validateString(next, descriptor))
	                            props.onChange(JsonPrimitive(next))
	                        }
	                    }
	                }

                is SemverConstraintsDescriptorDto -> {
                    val currentText = props.value?.asStringOrNull() ?: descriptor.minimum

	                    TextField {
	                        label = ReactNode(uiHints.label ?: props.fieldType.name)
	                        placeholder = uiHints.placeholder ?: descriptor.minimum
	                        value = currentText
	                        error = textError != null
	                        helperText = textError?.let(::ReactNode) ?: uiHints.helpText?.let(::ReactNode)

	                        unsafeOnChange { event: dynamic ->
	                            val next = event.target.asDynamic().value as? String ?: ""
	                            setTextError(validateSemver(next, descriptor))
	                            props.onChange(JsonPrimitive(next))
	                        }
	                    }
	                }

                is MapConstraintsDescriptorDto -> {
                    axesEditor(props, axesRows, setAxesRows)
                }

                is SchemaRefDescriptorDto -> {
                    jsonTextEditor(
                        props = props,
                        text = jsonText,
                        onTextChange = setJsonText,
                        error = jsonError,
                        onErrorChange = setJsonError,
                        titleSuffix = " (${descriptor.ref})",
                    )
                }
            }
        }
    }

private fun validateString(
    value: String,
    descriptor: StringConstraintsDescriptorDto,
): String? =
    when {
        descriptor.minLength != null && value.length < descriptor.minLength -> "Minimum length: ${descriptor.minLength}"
        descriptor.maxLength != null && value.length > descriptor.maxLength -> "Maximum length: ${descriptor.maxLength}"
        descriptor.pattern != null && !Regex(descriptor.pattern).matches(value) -> "Does not match pattern: ${descriptor.pattern}"
        else -> null
    }

private fun validateSemver(
    value: String,
    descriptor: SemverConstraintsDescriptorDto,
): String? {
    val pattern = descriptor.pattern?.let(::Regex)
    if (pattern != null && !pattern.matches(value)) {
        return "Invalid semver (pattern mismatch)"
    }

    val parsed = Semver.parse(value) ?: return "Invalid semver"
    val minimum = Semver.parse(descriptor.minimum) ?: return "Invalid minimum semver '${descriptor.minimum}'"

    return if (descriptor.allowAnyAboveMinimum && parsed < minimum) {
        "Must be >= ${descriptor.minimum}"
    } else {
        null
    }
}

private fun ChildrenBuilder.enumMultiSelect(
    props: FieldEditorProps,
    descriptor: EnumOptionsDescriptorDto,
) {
    val selected =
        (props.value as? JsonArray)
            ?.mapNotNull { it.asStringOrNull() }
            ?.toSet()
            ?: emptySet()

    FormControl {
        fullWidth = true
        InputLabel { +(descriptor.uiHints.label ?: props.fieldType.name) }

        Select {
            multiple = true
            value = selected.toTypedArray()
            onChange = { event, _ ->
                val values: Array<String> = event.target.asDynamic().value.unsafeCast<Array<String>>()
                props.onChange(JsonArray(values.distinct().sorted().map(::JsonPrimitive)))
            }
            renderValue = { value ->
                val entries = value.unsafeCast<Array<String>>().sorted()
                ReactNode(entries.joinToString(", "))
            }

            descriptor.options.forEach { option ->
                MenuItem {
                    value = option.value
                    +option.label
                }
            }
        }

        descriptor.uiHints.helpText?.let { help -> FormHelperText { +help } }
    }
}

private fun ChildrenBuilder.enumSelect(
    props: FieldEditorProps,
    descriptor: EnumOptionsDescriptorDto,
) {
    val selected = props.value?.asStringOrNull() ?: descriptor.options.firstOrNull()?.value.orEmpty()

    FormControl {
        fullWidth = true
        InputLabel { +(descriptor.uiHints.label ?: props.fieldType.name) }

        Select {
            value = selected
            onChange = { event, _ ->
                val next = event.target.asDynamic().value as? String ?: ""
                props.onChange(JsonPrimitive(next))
            }
            descriptor.options.forEach { option ->
                MenuItem {
                    value = option.value
                    +option.label
                }
            }
        }

        descriptor.uiHints.helpText?.let { help -> FormHelperText { +help } }
    }
}

private fun ChildrenBuilder.jsonTextEditor(
    props: FieldEditorProps,
    text: String,
    onTextChange: StateSetter<String>,
    error: String?,
    onErrorChange: StateSetter<String?>,
    titleSuffix: String,
	) {
	    TextField {
	        label = ReactNode((props.descriptor.uiHints.label ?: props.fieldType.name) + titleSuffix)
	        placeholder = props.descriptor.uiHints.placeholder ?: "{}"
	        value = text
        multiline = true
        minRows = 6
	        sx { fontFamily = FontFamily.monospace }
	        this.error = error != null
	        helperText = error?.let(::ReactNode) ?: props.descriptor.uiHints.helpText?.let(::ReactNode)

	        unsafeOnChange { event: dynamic ->
	            val next = event.target.asDynamic().value as? String ?: ""
	            onTextChange(next)

	            runCatching { Json.parseToJsonElement(next) }
	                .onSuccess {
                    onErrorChange(null)
                    props.onChange(it)
                }.onFailure {
                    onErrorChange(it.message ?: "Invalid JSON")
                }
        }
    }
}

private fun ChildrenBuilder.axesEditor(
    props: FieldEditorProps,
    rows: List<AxisRow>,
    setRows: StateSetter<List<AxisRow>>,
) {
    fun commit(nextRows: List<AxisRow>) {
        val map =
            nextRows
                .filter { it.axisId.isNotBlank() }
                .associate { row ->
                    val values =
                        row.allowedCsv
                            .split(",")
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
                    row.axisId to JsonArray(values.map(::JsonPrimitive))
                }
        props.onChange(JsonObject(map))
    }

    Stack {
        rows.forEachIndexed { idx, row ->
            Box {
                sx {
                    display = Display.flex
                    flexDirection = FlexDirection.row
                    alignItems = AlignItems.center
                    gap = 8.px
                    width = 100.pct
                }

	                TextField {
	                    label = ReactNode("Axis ID")
	                    value = row.axisId
	                    unsafeOnChange { event: dynamic ->
	                        val nextAxisId = event.target.asDynamic().value as? String ?: ""
	                        val nextRows = rows.mapIndexed { i, r -> if (i == idx) r.copy(axisId = nextAxisId) else r }
	                        setRows(nextRows)
	                        commit(nextRows)
	                    }
	                }

	                TextField {
	                    label = ReactNode("Allowed IDs (comma-separated)")
	                    value = row.allowedCsv
	                    unsafeOnChange { event: dynamic ->
	                        val nextAllowed = event.target.asDynamic().value as? String ?: ""
	                        val nextRows = rows.mapIndexed { i, r -> if (i == idx) r.copy(allowedCsv = nextAllowed) else r }
	                        setRows(nextRows)
	                        commit(nextRows)
	                    }
	                    sx { flexGrow = 1.0.unsafeCast<FlexGrow>() }
	                }

	                Button {
	                    variant = ButtonVariant.outlined
	                    color = ButtonColor.error
                    onClick = {
                        val nextRows = rows.filterIndexed { i, _ -> i != idx }
                        setRows(nextRows)
                        commit(nextRows)
                    }
                    +"Remove"
                }
            }
        }

        Box {
            Button {
                variant = ButtonVariant.outlined
                onClick = {
                    val nextRows = rows + AxisRow(axisId = "", allowedCsv = "")
                    setRows(nextRows)
                }
                +"Add axis"
            }
        }
    }
}

private data class AxisRow(
    val axisId: String,
    val allowedCsv: String,
)

private data class Semver(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Semver> {
    override fun compareTo(other: Semver): Int =
        compareValuesBy(this, other, Semver::major, Semver::minor, Semver::patch)

    companion object {
        private val basicPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+).*$")

        fun parse(value: String): Semver? =
            basicPattern.matchEntire(value)
                ?.groupValues
                ?.let { groups ->
                    val major = groups.getOrNull(1)?.toIntOrNull()
                    val minor = groups.getOrNull(2)?.toIntOrNull()
                    val patch = groups.getOrNull(3)?.toIntOrNull()
                    if (major != null && minor != null && patch != null) {
                        Semver(major, minor, patch)
                    } else {
                        null
                    }
                }
    }
}
