package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.model.FieldTypeDto
import mui.material.Paper
import mui.material.Table
import mui.material.TableBody
import mui.material.TableCell
import mui.material.TableContainer
import mui.material.TableHead
import mui.material.TableRow
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import web.cssom.FontFamily
import web.cssom.px

external interface BindingsTableProps : Props {
    var bindings: Map<String, FieldTypeDto>
}

val BindingsTable: FC<BindingsTableProps> =
    FC { props ->
        val sortedBindings = props.bindings.entries.sortedBy { it.key }

        Paper {
            sx { marginBottom = 16.px }

            Typography {
                sx {
                    fontWeight = web.cssom.integer(600)
                    padding = 16.px
                    paddingBottom = 8.px
                }
                +"Field Bindings"
            }

            TableContainer {
                Table {
                    size = mui.material.Size.small

                    TableHead {
                        TableRow {
                            TableCell {
                                +"JSON Pointer Template"
                            }
                            TableCell {
                                +"Field Type"
                            }
                        }
                    }

                    TableBody {
                        sortedBindings.forEach { (template, fieldType) ->
                            TableRow {
                                TableCell {
                                    Typography {
                                        sx {
                                            fontFamily = FontFamily.monospace
                                            fontSize = 13.px
                                        }
                                        +template
                                    }
                                }
                                TableCell {
                                    Typography {
                                        sx { fontSize = 13.px }
                                        +fieldType.name
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
