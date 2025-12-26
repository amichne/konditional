package io.amichne.konditional.ui.components

import io.amichne.konditional.ui.validation.Severity
import io.amichne.konditional.ui.validation.ValidationError
import mui.icons.material.Error
import mui.icons.material.Info
import mui.icons.material.Warning
import mui.material.Alert
import mui.material.AlertTitle
import mui.material.Box
import mui.material.Chip
import mui.material.ChipColor
import mui.material.Link
import mui.material.List
import mui.material.ListItem
import mui.material.ListItemIcon
import mui.material.ListItemText
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.create
import react.dom.events.MouseEvent
import react.useMemo
import react.useState
import web.cssom.Color
import web.cssom.Cursor
import web.cssom.FontFamily
import web.cssom.px
import web.html.HTMLElement

external interface ValidationErrorSummaryProps : Props {
    var errors: List<ValidationError>
    var onErrorClick: ((ValidationError) -> Unit)?
    var maxVisible: Int?
}

val ValidationErrorSummary: FC<ValidationErrorSummaryProps> =
    FC { props ->
        val (expanded, setExpanded) = useState(false)
        val maxVisible = props.maxVisible ?: 5

        val groupedErrors = useMemo(props.errors) {
            props.errors.groupBy { it.severity }
        }

        val errorCount = groupedErrors[Severity.ERROR]?.size ?: 0
        val warningCount = groupedErrors[Severity.WARNING]?.size ?: 0
        val infoCount = groupedErrors[Severity.INFO]?.size ?: 0

        val totalErrors = props.errors.size
        val visibleErrors = if (expanded) props.errors else props.errors.take(maxVisible)

        if (props.errors.isEmpty()) return@FC

        Alert {
            asDynamic().severity = when {
                errorCount > 0 -> "error"
                warningCount > 0 -> "warning"
                else -> "info"
            }

            AlertTitle {
                Box {
                    sx {
                        display = web.cssom.Display.flex
                        alignItems = web.cssom.AlignItems.center
                        gap = 8.px
                    }

                    Typography {
                        sx { fontWeight = web.cssom.integer(600) }
                        +"Validation Issues"
                    }

                    if (errorCount > 0) {
                        Chip {
                            icon = Error.create()
                            label = ReactNode("$errorCount error(s)")
                            asDynamic().size = "small"
                            color = ChipColor.error
                        }
                    }

                    if (warningCount > 0) {
                        Chip {
                            icon = Warning.create()
                            label = ReactNode("$warningCount warning(s)")
                            asDynamic().size = "small"
                            color = ChipColor.warning
                        }
                    }

                    if (infoCount > 0) {
                        Chip {
                            icon = Info.create()
                            label = ReactNode("$infoCount info")
                            asDynamic().size = "small"
                            color = ChipColor.info
                        }
                    }
                }
            }

            List {
                dense = true

                visibleErrors.forEach { error ->
                    ListItem {
                        disablePadding = true
                        sx { paddingLeft = 0.px }

                        ListItemIcon {
                            sx { minWidth = 32.px }
                            when (error.severity) {
                                Severity.ERROR -> Error { sx { color = Color("#d32f2f") } }
                                Severity.WARNING -> Warning { sx { color = Color("#ed6c02") } }
                                Severity.INFO -> Info { sx { color = Color("#0288d1") } }
                            }
                        }

                        ListItemText {
                            primary = ReactNode(error.message)
                            secondary = ReactNode(error.path.ifEmpty { "/" })
                        }
                    }
                }

                // Show more/less toggle
                if (totalErrors > maxVisible) {
                    ListItem {
                        disablePadding = true

                        Link {
                            sx {
                                cursor = Cursor.pointer
                                fontSize = 13.px
                            }
                            onClick = { setExpanded(!expanded) }
                            if (expanded) {
                                +"Show less"
                            } else {
                                +"Show ${totalErrors - maxVisible} more..."
                            }
                        }
                    }
                }
            }
        }
    }
