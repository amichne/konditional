package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.json.JsonPointer
import io.amichne.konditional.ui.model.FieldDescriptorDto
import io.amichne.konditional.ui.model.FieldTypeDto
import io.amichne.konditional.ui.model.SupportedValuesDto
import io.amichne.konditional.ui.validation.ValidationError
import io.amichne.konditional.ui.validation.validate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import mui.material.Alert
import mui.material.Paper
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.useEffect
import react.useMemo
import react.useState
import web.cssom.Color
import web.cssom.FontFamily
import web.cssom.Overflow
import web.cssom.WhiteSpace
import web.cssom.px

external interface EditableFieldsPanelProps : Props {
    var initialSnapshot: JsonElement
    var supportedValues: SupportedValuesDto
    var onSnapshotChange: ((JsonElement) -> Unit)?
}

private val jsonPretty: Json = Json { prettyPrint = true }

val EditableFieldsPanel: FC<EditableFieldsPanelProps> =
    FC { props ->
        val (snapshot, setSnapshot) = useState(props.initialSnapshot)
        val (error, setError) = useState<String?>(null)
        val (validationErrors, setValidationErrors) = useState<List<ValidationError>>(emptyList())

        useEffect(props.initialSnapshot) {
            setSnapshot(props.initialSnapshot)
            setError(null)
        }

        val boundFields =
            useMemo(snapshot, props.supportedValues) {
                bindEditableFields(snapshot, props.supportedValues)
            }

        // Run validation on all fields
        useEffect(snapshot, boundFields) {
            val allErrors = boundFields.flatMap { bound ->
                val result = validate(bound.value, bound.descriptor, bound.pointer)
                result.errors()
            }
            setValidationErrors(allErrors)
        }

        Stack {
            sx { gap = 16.px }

            Typography {
                sx { fontWeight = web.cssom.integer(600); fontSize = 18.px }
                +"Editable Fields"
            }

            // Validation error summary
            if (validationErrors.isNotEmpty()) {
                ValidationErrorSummary {
                    errors = validationErrors
                }
            }

            error?.let { message ->
                Alert {
                    asDynamic().severity = "error"
                    +message
                }
            }

            boundFields.forEach { bound ->
                Paper {
                    sx { padding = 16.px }

                    Stack {
                        sx { gap = 8.px }

                        Typography {
                            sx {
                                fontFamily = FontFamily.monospace
                                fontSize = 12.px
                                color = Color("#666")
                            }
                            +bound.pointer
                        }

                        FieldEditor {
                            fieldType = bound.fieldType
                            descriptor = bound.descriptor
                            value = bound.value
                            path = bound.pointer
                            this.validationErrors = validationErrors
                            onChange = { next ->
                                JsonPointer.set(snapshot, bound.pointer, next)
                                    .onSuccess {
                                        setError(null)
                                        setSnapshot(it)
                                        props.onSnapshotChange?.invoke(it)
                                    }.onFailure {
                                        setError(it.message ?: "Failed to update snapshot at ${bound.pointer}")
                                    }
                            }
                        }
                    }
                }
            }

            Paper {
                sx { padding = 16.px }
                Stack {
                    Typography {
                        sx { fontWeight = web.cssom.integer(500); marginBottom = 8.px }
                        +"Edited snapshot (client-side)"
                    }
                    Typography {
                        component = ReactHTML.pre
                        sx {
                            fontFamily = FontFamily.monospace
                            fontSize = 12.px
                            whiteSpace = WhiteSpace.preWrap
                            backgroundColor = Color("#f5f5f5")
                            padding = 12.px
                            borderRadius = 4.px
                            overflow = "auto".unsafeCast<Overflow>()
                            maxHeight = 400.px
                        }
                        +jsonPretty.encodeToString(JsonElement.serializer(), snapshot)
                    }
                }
            }
        }
    }

private data class BoundField(
    val pointer: String,
    val fieldType: FieldTypeDto,
    val descriptor: FieldDescriptorDto,
    val value: JsonElement?,
)

private fun bindEditableFields(
    snapshot: JsonElement,
    supportedValues: SupportedValuesDto,
): List<BoundField> =
    supportedValues.bindings.entries
        .flatMap { (template, fieldType) ->
            JsonPointer.expandTemplate(snapshot, template)
                .getOrElse { emptyList() }
                .map { pointer ->
                    val descriptor = supportedValues.byType[fieldType.name]
                    val value = JsonPointer.get(snapshot, pointer).getOrNull()
                    val finalValue =
                        if (value is JsonNull) {
                            null
                        } else {
                            value
                        }

                    descriptor?.let {
                        BoundField(
                            pointer = pointer,
                            fieldType = fieldType,
                            descriptor = it,
                            value = finalValue ?: JsonPrimitive(""),
                        )
                    }
                }.filterNotNull()
        }.sortedWith(compareBy({ it.fieldType.name }, { it.pointer }))
