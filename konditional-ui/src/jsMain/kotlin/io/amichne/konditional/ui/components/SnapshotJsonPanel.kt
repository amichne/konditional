package io.amichne.konditional.ui.components

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mui.icons.material.ContentCopy
import mui.material.Box
import mui.material.IconButton
import mui.material.Paper
import mui.material.Snackbar
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.dom.html.ReactHTML
import react.useState
import web.cssom.Color
import web.cssom.FontFamily
import web.cssom.JustifyContent
import web.cssom.Overflow
import web.cssom.WhiteSpace
import web.cssom.px

external interface SnapshotJsonPanelProps : Props {
    var snapshot: JsonElement
    var title: String?
}

private val jsonPretty: Json = Json { prettyPrint = true }

val SnapshotJsonPanel: FC<SnapshotJsonPanelProps> =
    FC { props ->
        val (copySuccess, setCopySuccess) = useState(false)

        val jsonString = jsonPretty.encodeToString(JsonElement.serializer(), props.snapshot)

        fun handleCopy() {
            kotlinx.browser.window.navigator.clipboard.writeText(jsonString)
                .then {
                    setCopySuccess(true)
                }
        }

        Paper {
            sx { padding = 16.px }

            Stack {
                Box {
                    sx {
                        display = web.cssom.Display.flex
                        justifyContent = JustifyContent.spaceBetween
                        alignItems = web.cssom.AlignItems.center
                        marginBottom = 8.px
                    }

                    Typography {
                        sx { fontWeight = web.cssom.integer(500) }
                        +(props.title ?: "Configuration Snapshot")
                    }

                    IconButton {
                        title = "Copy to clipboard"
                        onClick = { handleCopy() }
                        ContentCopy {}
                    }
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
                        maxHeight = 500.px
                        margin = 0.px
                    }
                    +jsonString
                }
            }

            Snackbar {
                open = copySuccess
                autoHideDuration = 2000
                onClose = { _, _ -> setCopySuccess(false) }
                message = ReactNode("Copied to clipboard!")
            }
        }
    }
