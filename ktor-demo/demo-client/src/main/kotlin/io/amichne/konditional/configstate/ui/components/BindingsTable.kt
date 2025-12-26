package io.amichne.konditional.configstate.ui.components

import io.amichne.konditional.configstate.ui.model.FieldTypeDto
import mui.material.Paper
import mui.material.Size
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
import web.cssom.px

external interface BindingsTableProps : Props {
    var bindings: Map<String, FieldTypeDto>
}

val BindingsTable: FC<BindingsTableProps> =
    FC { props ->
        TableContainer {
            component = Paper
            sx { padding = 12.px }

            Typography {
                +"Bindings (JSON Pointer template → FieldType)"
            }

            Table {
                size = Size.small
                TableHead {
                    TableRow {
                        TableCell { +"Pointer template" }
                        TableCell { +"FieldType" }
                    }
                }
                TableBody {
                    props.bindings.entries.sortedBy { it.key }.forEach { (template, fieldType) ->
                        TableRow {
                            TableCell { Typography { +template } }
                            TableCell { Typography { +fieldType.name } }
                        }
                    }
                }
            }
        }
    }
