package io.amichne.konditional.demo.evaluation

import mui.icons.material.CheckCircle
import mui.icons.material.Cancel
import mui.material.*
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.useEffectOnce
import react.useState
import web.cssom.*
import kotlin.js.Json

/**
 * Displays rules metadata loaded from window.KONDITIONAL_RULES
 */
val RulesInfoPanel: FC<Props> = FC {
    val (rulesData, setRulesData) = useState<List<RuleMetadata>?>(null)
    val (error, setError) = useState<String?>(null)

    useEffectOnce {
        try {
            val data: dynamic = js("window.KONDITIONAL_RULES")

            if (data == null || js("typeof data === 'undefined'") == true) {
                setError("No rules data available")
                return@useEffectOnce
            }

            val featuresArray = data.features as? Array<*>
            if (featuresArray == null) {
                setError("Invalid rules data format")
                return@useEffectOnce
            }

            val rules = featuresArray.mapNotNull { featureData ->
                val feature = featureData.unsafeCast<Json>()
                try {
                    RuleMetadata(
                        key = feature["key"].unsafeCast<String?>() ?: "unknown",
                        type = feature["type"].unsafeCast<String?>() ?: "Unknown",
                        default_ = feature["default"],
                        rulesCount = feature["rulesCount"].unsafeCast<Number?>()?.toInt() ?: 0,
                        hasRules = feature["hasRules"].unsafeCast<Boolean?>() ?: false
                    )
                } catch (e: Exception) {
                    console.error("Failed to parse rule metadata:", e)
                    null
                }
            }

            setRulesData(rules)
        } catch (e: Exception) {
            console.error("Error loading rules:", e)
            setError("Error loading rules: ${e.message}")
        }
    }

    when {
        error != null -> {
            Alert {
                asDynamic().severity = "warning"
                +error
            }
        }

        rulesData == null -> {
            Box {
                sx { textAlign = TextAlign.center; padding = 16.px }
                CircularProgress { size = 24 }
            }
        }

        rulesData.isEmpty() -> {
            Typography {
                sx { color = Color("#666"); fontStyle = FontStyle.italic }
                +"No rules configured"
            }
        }

        else -> {
            Stack {
                sx { gap = 12.px }

                rulesData.forEach { rule ->
                    RuleInfoCard {
                        metadata = rule
                    }
                }
            }
        }
    }
}

external interface RuleInfoCardProps : Props {
    var metadata: RuleMetadata
}

val RuleInfoCard: FC<RuleInfoCardProps> = FC { props ->
    Card {
        variant = PaperVariant.outlined
        sx {
            padding = 12.px
            this.asDynamic()["&:hover"] = js("{backgroundColor: '#f5f5f5'}")
        }

        Stack {
            sx { gap = 8.px }

            // Header with name and type badge
            Box {
                sx {
                    display = Display.flex
                    justifyContent = JustifyContent.spaceBetween
                    alignItems = AlignItems.center
                }

                Typography {
                    sx { fontWeight = web.cssom.integer(600); fontFamily = FontFamily.monospace; fontSize = 12.px }
                    +props.metadata.key
                }

                Chip {
                    label = ReactNode(props.metadata.type)
                    size = Size.small
                    asDynamic().color = when (props.metadata.type.lowercase()) {
                        "boolean" -> "primary"
                        "string" -> "secondary"
                        "int", "double" -> "success"
                        else -> "default"
                    }
                }
            }

            // Default value
            Typography {
                sx { color = Color("#666"); fontSize = 11.px }

                val defaultDisplay = when (val default_ = props.metadata.default_) {
                    is String -> "\"$default_\""
                    else -> default_.toString()
                }
                +"Default: $defaultDisplay"
            }

            // Rules indicator
            Box {
                sx {
                    display = Display.flex
                    alignItems = AlignItems.center
                    gap = 4.px
                }

                if (props.metadata.hasRules) {
                    CheckCircle {
                        sx {
                            fontSize = 16.px
                            color = Color("#2e7d32")
                        }
                    }
                    Typography {
                        sx { color = Color("#2e7d32"); fontWeight = web.cssom.integer(500); fontSize = 11.px }
                        +"${props.metadata.rulesCount} rule(s) configured"
                    }
                } else {
                    Cancel {
                        sx {
                            fontSize = 16.px
                            color = Color("#666")
                        }
                    }
                    Typography {
                        sx { color = Color("#666"); fontSize = 11.px }
                        +"No rules"
                    }
                }
            }
        }
    }
}

data class RuleMetadata(
    val key: String,
    val type: String,
    val default_: Any?,
    val rulesCount: Int,
    val hasRules: Boolean,
)
