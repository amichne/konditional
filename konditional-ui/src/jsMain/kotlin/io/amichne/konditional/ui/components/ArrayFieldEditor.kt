package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.defaults.DefaultValueGenerator
import io.amichne.konditional.ui.model.ArrayDescriptorDto
import io.amichne.konditional.ui.model.FieldTypeDto
import io.amichne.konditional.ui.validation.ValidationError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import mui.icons.material.Add
import mui.icons.material.ArrowDownward
import mui.icons.material.ArrowUpward
import mui.icons.material.Delete
import mui.material.Alert
import mui.material.Box
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Card
import mui.material.CardContent
import mui.material.IconButton
import mui.material.IconButtonColor
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.create
import react.useMemo
import web.cssom.AlignItems
import web.cssom.Color
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.FlexGrow
import web.cssom.JustifyContent
import web.cssom.px

external interface ArrayFieldEditorProps : Props {
    var descriptor: ArrayDescriptorDto
    var value: JsonArray
    var onChange: (JsonElement) -> Unit
    var path: String
    var validationErrors: List<ValidationError>?
}

val ArrayFieldEditor: FC<ArrayFieldEditorProps> =
    FC { props ->
        val items = props.value
        val descriptor = props.descriptor

        // Get array-level validation errors
        val arrayErrors = useMemo(props.validationErrors, props.path) {
            props.validationErrors?.filter { it.path == props.path } ?: emptyList()
        }

        // Handle add item
        fun handleAdd() {
            val newItem = DefaultValueGenerator.generate(descriptor.itemDescriptor)
            props.onChange(JsonArray(items + newItem))
        }

        // Handle remove item
        fun handleRemove(index: Int) {
            props.onChange(JsonArray(items.filterIndexed { i, _ -> i != index }))
        }

        // Handle item change
        fun handleItemChange(index: Int, newValue: JsonElement) {
            val newItems = items.mapIndexed { i, item -> if (i == index) newValue else item }
            props.onChange(JsonArray(newItems))
        }

        // Handle move up
        fun handleMoveUp(index: Int) {
            if (index <= 0) return
            val newItems = items.toMutableList()
            val item = newItems.removeAt(index)
            newItems.add(index - 1, item)
            props.onChange(JsonArray(newItems))
        }

        // Handle move down
        fun handleMoveDown(index: Int) {
            if (index >= items.size - 1) return
            val newItems = items.toMutableList()
            val item = newItems.removeAt(index)
            newItems.add(index + 1, item)
            props.onChange(JsonArray(newItems))
        }

        Stack {
            sx { gap = 16.px }

            // Header with constraints info
            Box {
                sx {
                    display = Display.flex
                    justifyContent = JustifyContent.spaceBetween
                    alignItems = AlignItems.center
                }

                Typography {
                    sx { fontWeight = web.cssom.integer(500) }
                    +(descriptor.uiHints.label ?: "Items")
                }

                Typography {
                    sx { fontSize = 12.px; color = Color("#666") }
                    val constraintParts = mutableListOf<String>()
                    descriptor.minItems?.let { constraintParts += "min: $it" }
                    descriptor.maxItems?.let { constraintParts += "max: $it" }
                    if (descriptor.uniqueItems) constraintParts += "unique"
                    if (constraintParts.isNotEmpty()) {
                        +"(${constraintParts.joinToString(", ")})"
                    }
                }
            }

            // Array-level validation errors
            arrayErrors.forEach { error ->
                Alert {
                    asDynamic().severity = "error"
                    +error.message
                }
            }

            // Items list
            items.forEachIndexed { index, item ->
                val itemPath = "${props.path}/$index"
                val itemErrors = props.validationErrors?.filter {
                    it.path == itemPath || it.path.startsWith("$itemPath/")
                } ?: emptyList()
                val canRemove = descriptor.minItems?.let { items.size > it } ?: true

                Card {
                    sx {
                        if (itemErrors.isNotEmpty()) {
                            borderColor = Color("#d32f2f")
                            borderWidth = 1.px
                            borderStyle = web.cssom.LineStyle.solid
                        }
                    }

                    CardContent {
                        Box {
                            sx {
                                display = Display.flex
                                flexDirection = FlexDirection.row
                                alignItems = AlignItems.flexStart
                                gap = 8.px
                            }

                            // Reorder controls
                            Stack {
                                sx {
                                    flexDirection = FlexDirection.column
                                    alignItems = AlignItems.center
                                }

                                IconButton {
                                    size = mui.material.Size.small
                                    disabled = index == 0
                                    onClick = { handleMoveUp(index) }
                                    title = "Move up"
                                    ArrowUpward { fontSize = mui.material.SvgIconSize.small }
                                }

                                Typography {
                                    sx { fontSize = 12.px; color = Color("#999") }
                                    +"${index + 1}"
                                }

                                IconButton {
                                    size = mui.material.Size.small
                                    disabled = index == items.size - 1
                                    onClick = { handleMoveDown(index) }
                                    title = "Move down"
                                    ArrowDownward { fontSize = mui.material.SvgIconSize.small }
                                }
                            }

                            // Field editor
                            Box {
                                sx { flexGrow = 1.0.unsafeCast<FlexGrow>() }

                                FieldEditor {
                                    fieldType = FieldTypeDto.FLAG_VALUE
                                    this.descriptor = props.descriptor.itemDescriptor
                                    value = item
                                    onChange = { newValue -> handleItemChange(index, newValue) }
                                    path = itemPath
                                    validationErrors = props.validationErrors
                                }
                            }

                            // Remove button
                            IconButton {
                                color = IconButtonColor.error
                                disabled = !canRemove
                                onClick = { handleRemove(index) }
                                title = "Remove item"
                                Delete {}
                            }
                        }
                    }
                }
            }

            // Add button
            Box {
                Button {
                    variant = ButtonVariant.outlined
                    startIcon = Add.create()
                    disabled = descriptor.maxItems?.let { items.size >= it } ?: false
                    onClick = { handleAdd() }
                    +"Add item"
                }
            }
        }
    }
