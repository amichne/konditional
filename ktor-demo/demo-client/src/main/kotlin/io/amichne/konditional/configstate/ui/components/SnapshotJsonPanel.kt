package io.amichne.konditional.configstate.ui.components

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mui.material.Paper
import mui.material.Stack
import mui.material.Typography
import mui.system.sx
import react.FC
import react.Props
import react.dom.html.ReactHTML
import web.cssom.FontFamily
import web.cssom.WhiteSpace
import web.cssom.px

external interface SnapshotJsonPanelProps : Props {
    var snapshot: JsonElement
}

private val jsonPretty: Json = Json { prettyPrint = true }

val SnapshotJsonPanel: FC<SnapshotJsonPanelProps> =
    FC { props ->
        Paper {
            sx { padding = 16.px }

            Stack {
                Typography { +"Snapshot (read-only JSON)" }
                Typography {
                    component = ReactHTML.pre
                    sx {
                        fontFamily = FontFamily.monospace
                        fontSize = 12.px
                        whiteSpace = WhiteSpace.preWrap
                    }
                    +jsonPretty.encodeToString(JsonElement.serializer(), props.snapshot)
                }
            }
        }
    }
