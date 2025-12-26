package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.defaults.DefaultValueGenerator
import io.amichne.konditional.ui.model.FieldTypeDto
import io.amichne.konditional.ui.model.ObjectDescriptorDto
import io.amichne.konditional.ui.validation.ValidationError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mui.icons.material.Add
import mui.icons.material.Delete
import mui.icons.material.ExpandMore
import mui.material.Accordion
import mui.material.AccordionDetails
import mui.material.AccordionSummary
import mui.material.Alert
import mui.material.Box
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Chip
import mui.material.ChipColor
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Stack
import mui.material.TextField
import mui.material.TextFieldProps
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.create
import react.useMemo
import react.useState
import web.cssom.AlignItems
import web.cssom.Color
import web.cssom.Display
import web.cssom.FlexGrow
import web.cssom.JustifyContent
import web.cssom.pct
import web.cssom.px

external interface ObjectFieldEditorProps : Props {
    var descriptor: ObjectDescriptorDto
    var value: JsonObject
    var onChange: (JsonElement) -> Unit
    var path: String
    var validationErrors: List<ValidationError>?
}

private fun TextFieldProps.unsafeOnChange(handler: (event: dynamic) -> Unit) {
    asDynamic().onChange = handler
}

val ObjectFieldEditor: FC<ObjectFieldEditorProps> =
    FC { props ->
        val obj = props.value
        val descriptor = props.descriptor

        // State for new additional property
        val (newPropKey, setNewPropKey) = useState("")
        val (newPropKeyError, setNewPropKeyError) = useState<String?>(null)

        // Sort properties by order
        val sortedProperties = useMemo(descriptor.properties) {
            descriptor.properties.entries.sortedBy { it.value.order }
        }

        // Object-level validation errors
        val objectErrors = useMemo(props.validationErrors, props.path) {
            props.validationErrors?.filter { it.path == props.path } ?: emptyList()
        }

        // Get additional property keys (keys not in defined properties)
        val additionalKeys = useMemo(obj, descriptor.properties) {
            obj.keys.filter { it !in descriptor.properties.keys }.sorted()
        }

        // Handle property change
        fun handlePropertyChange(key: String, newValue: JsonElement) {
            val newObj = obj.toMutableMap().apply { put(key, newValue) }
            props.onChange(JsonObject(newObj))
        }

        // Handle property removal (for additional properties)
        fun handleRemoveProperty(key: String) {
            val newObj = obj.toMutableMap().apply { remove(key) }
            props.onChange(JsonObject(newObj))
        }

        // Handle add additional property
        fun handleAddAdditionalProperty() {
            if (newPropKey.isBlank()) {
                setNewPropKeyError("Key is required")
                return
            }
            if (newPropKey in obj.keys) {
                setNewPropKeyError("Key already exists")
                return
            }

            descriptor.additionalProperties?.let { additionalDesc ->
                val newValue = DefaultValueGenerator.generate(additionalDesc)
                val newObj = obj.toMutableMap().apply { put(newPropKey, newValue) }
                props.onChange(JsonObject(newObj))
                setNewPropKey("")
                setNewPropKeyError(null)
            }
        }

        Stack {
            sx { gap = 8.px }

            // Header
            Typography {
                sx { fontWeight = web.cssom.integer(500); marginBottom = 8.px }
                +(descriptor.uiHints.label ?: "Object Properties")
            }

            // Object-level validation errors
            objectErrors.forEach { error ->
                Alert {
                    asDynamic().severity = "error"
                    +error.message
                }
            }

            // Defined properties as accordions
            sortedProperties.forEach { (key, propDescriptor) ->
                val propPath = "${props.path}/$key"
                val propValue = obj[key]
                val isRequired = key in descriptor.required
                val isMissing = propValue == null && isRequired

                // Property-level errors
                val propErrors = props.validationErrors?.filter {
                    it.path == propPath || it.path.startsWith("$propPath/")
                } ?: emptyList()

                val hasError = propErrors.isNotEmpty() || isMissing

                Accordion {
                    sx {
                        if (hasError) {
                            borderColor = Color("#d32f2f")
                            borderWidth = 1.px
                            borderStyle = web.cssom.LineStyle.solid
                        }
                    }

                    AccordionSummary {
                        expandIcon = ExpandMore.create()

                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.center
                                gap = 8.px
                                width = 100.pct
                            }

                            Typography {
                                +key
                            }

                            if (isRequired) {
                                Chip {
                                    label = ReactNode("required")
                                    asDynamic().size = "small"
                                    color = if (isMissing) ChipColor.error else ChipColor.primary
                                }
                            }

                            if (propErrors.isNotEmpty()) {
                                Chip {
                                    label = ReactNode("${propErrors.size} error(s)")
                                    asDynamic().size = "small"
                                    color = ChipColor.error
                                }
                            }
                        }
                    }

                    AccordionDetails {
                        FieldEditor {
                            fieldType = FieldTypeDto.FLAG_VALUE
                            this.descriptor = propDescriptor.descriptor
                            value = propValue
                            onChange = { newValue -> handlePropertyChange(key, newValue) }
                            path = propPath
                            validationErrors = props.validationErrors
                        }
                    }
                }
            }

            // Additional properties section
            descriptor.additionalProperties?.let { additionalDesc ->
                if (additionalKeys.isNotEmpty() || descriptor.additionalProperties != null) {
                    Box {
                        sx { marginTop = 16.px }

                        Typography {
                            sx {
                                fontWeight = web.cssom.integer(500)
                                marginBottom = 8.px
                                color = Color("#666")
                            }
                            +"Additional Properties"
                        }

                        // Existing additional properties
                        additionalKeys.forEach { key ->
                            val propPath = "${props.path}/$key"
                            val propValue = obj[key]

                            val propErrors = props.validationErrors?.filter {
                                it.path == propPath || it.path.startsWith("$propPath/")
                            } ?: emptyList()

                            Accordion {
                                sx {
                                    if (propErrors.isNotEmpty()) {
                                        borderColor = Color("#d32f2f")
                                        borderWidth = 1.px
                                        borderStyle = web.cssom.LineStyle.solid
                                    }
                                }

                                AccordionSummary {
                                    expandIcon = ExpandMore.create()

                                    Box {
                                        sx {
                                            display = Display.flex
                                            alignItems = AlignItems.center
                                            justifyContent = JustifyContent.spaceBetween
                                            width = 100.pct
                                        }

                                        Typography {
                                            +key
                                        }

                                        IconButton {
                                            color = IconButtonColor.error
                                            size = mui.material.Size.small
                                            onClick = { e ->
                                                e.stopPropagation()
                                                handleRemoveProperty(key)
                                            }
                                            Delete {}
                                        }
                                    }
                                }

                                AccordionDetails {
                                    FieldEditor {
                                        fieldType = FieldTypeDto.FLAG_VALUE
                                        this.descriptor = additionalDesc
                                        value = propValue
                                        onChange = { newValue -> handlePropertyChange(key, newValue) }
                                        path = propPath
                                        validationErrors = props.validationErrors
                                    }
                                }
                            }
                        }

                        // Add new additional property
                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.flexStart
                                gap = 8.px
                                marginTop = 8.px
                            }

                            TextField {
                                label = ReactNode("New property key")
                                value = newPropKey
                                error = newPropKeyError != null
                                helperText = newPropKeyError?.let(::ReactNode)
                                size = mui.material.Size.small
                                sx { flexGrow = 1.0.unsafeCast<FlexGrow>() }
                                unsafeOnChange { event: dynamic ->
                                    val next = event.target.asDynamic().value as? String ?: ""
                                    setNewPropKey(next)
                                    setNewPropKeyError(null)
                                }
                            }

                            Button {
                                variant = ButtonVariant.outlined
                                startIcon = Add.create()
                                disabled = newPropKey.isBlank()
                                onClick = { handleAddAdditionalProperty() }
                                +"Add"
                            }
                        }
                    }
                }
            }
        }
    }
