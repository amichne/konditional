package io.amichne.konditional.configstate.ui.components

import io.amichne.konditional.configstate.ui.json.JsonPointer
import io.amichne.konditional.configstate.ui.model.FieldDescriptorDto
import io.amichne.konditional.configstate.ui.model.FieldTypeDto
import io.amichne.konditional.configstate.ui.model.SupportedValuesDto
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
import web.cssom.FontFamily
import web.cssom.WhiteSpace
import web.cssom.px

external interface EditableFieldsPanelProps : Props {
    var initialSnapshot: JsonElement
    var supportedValues: SupportedValuesDto
}

private val jsonPretty: Json = Json { prettyPrint = true }

val EditableFieldsPanel: FC<EditableFieldsPanelProps> =
    FC { props ->
        val (snapshot, setSnapshot) = useState(props.initialSnapshot)
        val (error, setError) = useState<String?>(null)

        useEffect(props.initialSnapshot) {
            setSnapshot(props.initialSnapshot)
            setError(null)
        }

        val boundFields =
            useMemo(snapshot, props.supportedValues) {
                bindEditableFields(snapshot, props.supportedValues)
            }

        Stack {
            Typography { +"Editable Fields (expanded from bindings)" }

            error?.let { message ->
                Alert {
                    severity = "error"
                    +message
                }
            }

            boundFields.forEach { bound ->
                Paper {
                    sx { padding = 16.px }

                    Stack {
                        Typography {
                            +bound.pointer
                        }

                        FieldEditor {
                            fieldType = bound.fieldType
                            descriptor = bound.descriptor
                            value = bound.value
                            onChange = { next ->
                                JsonPointer.set(snapshot, bound.pointer, next)
                                    .onSuccess {
                                        setError(null)
                                        setSnapshot(it)
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
                    Typography { +"Edited snapshot (client-side)" }
                    Typography {
                        component = ReactHTML.pre
                        sx {
                            fontFamily = FontFamily.monospace
                            fontSize = 12.px
                            whiteSpace = WhiteSpace.preWrap
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
