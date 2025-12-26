package io.amichne.konditional.demo.evaluation

import io.amichne.konditional.demo.net.EvaluationResponse
import mui.icons.material.CheckCircle
import mui.icons.material.Cancel
import mui.material.*
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.create
import web.cssom.*

external interface FeatureResultsGridProps : Props {
    var results: EvaluationResponse
    var contextType: String
}

val FeatureResultsGrid: FC<FeatureResultsGridProps> = FC { props ->
    Stack {
        sx { gap = 16.px }

        // Base features
        FeatureCard {
            name = "Dark Mode"
            value = props.results.darkMode
            type = FeatureValueType.BOOLEAN
        }

        FeatureCard {
            name = "Beta Features"
            value = props.results.betaFeatures
            type = FeatureValueType.BOOLEAN
        }

        FeatureCard {
            name = "Analytics Enabled"
            value = props.results.analyticsEnabled
            type = FeatureValueType.BOOLEAN
        }

        FeatureCard {
            name = "Welcome Message"
            value = props.results.welcomeMessage
            type = FeatureValueType.STRING
        }

        FeatureCard {
            name = "Theme Color"
            value = props.results.themeColor
            type = FeatureValueType.COLOR
        }

        FeatureCard {
            name = "Max Items Per Page"
            value = props.results.maxItemsPerPage
            type = FeatureValueType.NUMBER
        }

        FeatureCard {
            name = "Cache TTL (seconds)"
            value = props.results.cacheTtlSeconds
            type = FeatureValueType.NUMBER
        }

        FeatureCard {
            name = "Discount %"
            value = "${props.results.discountPercentage}%"
            type = FeatureValueType.NUMBER
        }

        FeatureCard {
            name = "API Rate Limit"
            value = props.results.apiRateLimit
            type = FeatureValueType.NUMBER
        }

        // Enterprise features
        if (props.contextType == "enterprise") {
            FeatureCard {
                name = "SSO Enabled"
                value = props.results.ssoEnabled
                type = FeatureValueType.BOOLEAN
            }

            FeatureCard {
                name = "Advanced Analytics"
                value = props.results.advancedAnalytics
                type = FeatureValueType.BOOLEAN
            }

            FeatureCard {
                name = "Custom Branding"
                value = props.results.customBranding
                type = FeatureValueType.BOOLEAN
            }

            FeatureCard {
                name = "Dedicated Support"
                value = props.results.dedicatedSupport
                type = FeatureValueType.BOOLEAN
            }
        }
    }
}

external interface FeatureCardProps : Props {
    var name: String
    var value: Any?
    var type: FeatureValueType
}

val FeatureCard: FC<FeatureCardProps> = FC { props ->
    Card {
        sx {
            padding = 16.px
            transition = "all 0.2s ease".unsafeCast<Transition>()
            this.asDynamic()["&:hover"] = js("{transform: 'translateY(-2px)', boxShadow: 3}")
        }

        CardContent {
            Stack {
                sx { gap = 8.px }

                Typography {
                    sx { color = Color("#666"); fontWeight = web.cssom.integer(500); fontSize = 14.px }
                    +props.name
                }

                when (props.type) {
                    FeatureValueType.BOOLEAN -> {
                        val enabled = props.value as? Boolean ?: false
                        Chip {
                            icon = if (enabled) CheckCircle.create() else Cancel.create()
                            label = ReactNode(if (enabled) "Enabled" else "Disabled")
                            asDynamic().color = if (enabled) "success" else "default"
                            size = Size.medium
                        }
                    }

                    FeatureValueType.COLOR -> {
                        Box {
                            sx {
                                display = Display.flex
                                alignItems = AlignItems.center
                                gap = 8.px
                            }

                            Box {
                                sx {
                                    width = 32.px
                                    height = 32.px
                                    borderRadius = 4.px
                                    backgroundColor = Color(props.value.toString())
                                    border = "1px solid #e0e0e0".unsafeCast<Border>()
                                }
                            }

                            Typography {
                                sx { fontFamily = FontFamily.monospace }
                                +props.value.toString()
                            }
                        }
                    }

                    FeatureValueType.STRING -> {
                        Typography {
                            sx {
                                fontWeight = web.cssom.integer(500)
                                color = Color("#1976d2")
                            }
                            +props.value.toString()
                        }
                    }

                    FeatureValueType.NUMBER -> {
                        Typography {
                            sx {
                                fontSize = 24.px
                                fontWeight = web.cssom.integer(600)
                                color = Color("#2e7d32")
                            }
                            +props.value.toString()
                        }
                    }
                }
            }
        }
    }
}

enum class FeatureValueType {
    BOOLEAN,
    STRING,
    NUMBER,
    COLOR,
}
