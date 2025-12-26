package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.defaults.DefaultValueGenerator
import io.amichne.konditional.ui.model.FieldDescriptorDto
import io.amichne.konditional.ui.model.FieldTypeDto
import kotlinx.serialization.json.JsonElement
import mui.material.Box
import mui.material.Card
import mui.material.CardContent
import mui.material.Paper
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.useState
import web.cssom.Color
import web.cssom.Display
import web.cssom.FlexWrap
import web.cssom.pct
import web.cssom.px

external interface DescriptorCatalogProps : Props {
    var descriptors: Map<String, FieldDescriptorDto>
}

val DescriptorCatalog: FC<DescriptorCatalogProps> =
    FC { props ->
        val sortedDescriptors = props.descriptors.entries.sortedBy { it.key }

        Paper {
            sx { padding = 16.px; marginBottom = 16.px }

            Typography {
                sx {
                    fontWeight = web.cssom.integer(600)
                    marginBottom = 16.px
                }
                +"Field Type Catalog"
            }

            Box {
                sx {
                    display = Display.flex
                    flexWrap = FlexWrap.wrap
                    gap = 16.px
                }

                sortedDescriptors.forEach { (typeName, descriptor) ->
                    Box {
                        sx {
                            width = 300.px
                            minWidth = 280.px
                        }

                        DescriptorPreviewCard {
                            this.typeName = typeName
                            this.descriptor = descriptor
                        }
                    }
                }
            }
        }
    }

external interface DescriptorPreviewCardProps : Props {
    var typeName: String
    var descriptor: FieldDescriptorDto
}

private val DescriptorPreviewCard: FC<DescriptorPreviewCardProps> =
    FC { props ->
        val initialValue = DefaultValueGenerator.sample(props.descriptor)
        val (value, setValue) = useState<JsonElement>(initialValue)

        Card {
            sx {
                height = 100.pct
            }

            CardContent {
                Stack {
                    sx { gap = 8.px }

                    Typography {
                        sx {
                            fontWeight = web.cssom.integer(500)
                            fontSize = 14.px
                            color = Color("#1976d2")
                        }
                        +props.typeName
                    }

                    FieldEditor {
                        fieldType = FieldTypeDto.FLAG_VALUE
                        descriptor = props.descriptor
                        this.value = value
                        onChange = { setValue(it) }
                    }
                }
            }
        }
    }
