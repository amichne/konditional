package io.amichne.konditional.demo.evaluation

import io.amichne.konditional.demo.net.EvaluationResponse
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mui.material.*
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.create
import react.dom.html.ReactHTML
import react.useState
import web.cssom.*
import org.w3c.fetch.RequestInit
import kotlin.js.json

/**
 * Main React app for feature evaluation demo
 * Replaces legacy DemoClient with MUI components
 */
val FeatureEvaluationApp: FC<Props> = FC {
    val (contextType, setContextType) = useState("base")
    val (locale, setLocale) = useState("UNITED_STATES")
    val (platform, setPlatform) = useState("WEB")
    val (version, setVersion) = useState("1.0.0")
    val (stableId, setStableId) = useState("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")

    // Enterprise-specific fields
    val (tier, setTier) = useState("FREE")
    val (orgId, setOrgId) = useState("org-001")
    val (employeeCount, setEmployeeCount) = useState("10")

    val (hotReload, setHotReload) = useState(false)
    val (results, setResults) = useState<EvaluationResponse?>(null)
    val (loading, setLoading) = useState(false)
    val (error, setError) = useState<String?>(null)

    // Auto-evaluate on changes when hot reload is enabled
    react.useEffect(contextType, locale, platform, version, stableId, tier, orgId, employeeCount, hotReload) {
        if (hotReload) {
            evaluateFeatures(
                contextType, locale, platform, version, stableId,
                tier, orgId, employeeCount,
                { setResults(it) }, { setLoading(it) }, { setError(it) }
            )
        }
    }

    Container {
        maxWidth = "xl"
        sx { paddingTop = 32.px; paddingBottom = 32.px }

        // Header
        Paper {
            sx { padding = 24.px; marginBottom = 24.px }

            Box {
                sx {
                    display = Display.flex
                    justifyContent = JustifyContent.spaceBetween
                    alignItems = AlignItems.center
                }

                Typography {
                    sx { fontSize = 24.px; fontWeight = web.cssom.integer(700); margin = 0.px }
                    +"🎯 Konditional Feature Evaluation Demo"
                }

                FormControlLabel {
                    control = Switch.create {
                        checked = hotReload
                        onChange = { _, checked -> setHotReload(checked) }
                    }
                    label = ReactNode("Hot Reload Mode")
                }
            }
        }

        Stack {
            sx { gap = 24.px }

            // Left column: Context form
            Paper {
                sx { padding = 24.px }

                Stack {
                    sx { gap = 16.px }

                    Typography {
                        sx { fontSize = 20.px; fontWeight = web.cssom.integer(600); marginTop = 0.px }
                        +"📝 Context Configuration"
                    }

                    // Context Type
                    FormControl {
                        fullWidth = true

                        InputLabel { +"Context Type" }
                        Select {
                            value = contextType.unsafeCast<Nothing?>()
                            label = ReactNode("Context Type")
                            onChange = { event, _ ->
                                setContextType(event.target.asDynamic().value as String)
                            }

                            MenuItem { value = "base"; +"Base Context" }
                            MenuItem { value = "enterprise"; +"Enterprise Context" }
                        }
                    }

                    // Locale
                    FormControl {
                        fullWidth = true

                        InputLabel { +"Locale" }
                        Select {
                            value = locale.unsafeCast<Nothing?>()
                            label = ReactNode("Locale")
                            onChange = { event, _ ->
                                setLocale(event.target.asDynamic().value as String)
                            }

                            MenuItem { value = "UNITED_STATES"; +"United States" }
                            MenuItem { value = "CANADA"; +"Canada" }
                            MenuItem { value = "INDIA"; +"India" }
                            MenuItem { value = "UNITED_KINGDOM"; +"United Kingdom" }
                        }
                    }

                    // Platform
                    FormControl {
                        fullWidth = true

                        InputLabel { +"Platform" }
                        Select {
                            value = platform.unsafeCast<Nothing?>()
                            label = ReactNode("Platform")
                            onChange = { event, _ ->
                                setPlatform(event.target.asDynamic().value as String)
                            }

                            MenuItem { value = "WEB"; +"Web" }
                            MenuItem { value = "IOS"; +"iOS" }
                            MenuItem { value = "ANDROID"; +"Android" }
                        }
                    }

                    // Version
                    TextField {
                        fullWidth = true
                        label = ReactNode("App Version")
                        value = version
                        asDynamic().onChange = { event: dynamic ->
                            setVersion(event.target.value as String)
                        }
                        placeholder = "1.0.0"
                    }

                    // Stable ID
                    FormControl {
                        fullWidth = true

                        InputLabel { +"Stable ID (User)" }
                        Select {
                            value = stableId.unsafeCast<Nothing?>()
                            label = ReactNode("Stable ID (User)")
                            onChange = { event, _ ->
                                setStableId(event.target.asDynamic().value as String)
                            }

                            MenuItem { value = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"; +"User Alpha" }
                            MenuItem { value = "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a1"; +"User Beta" }
                            MenuItem { value = "c3d4e5f6a7b8c9d0e1f2a3b4c5d6a1b2"; +"User Gamma" }
                            MenuItem { value = "d4e5f6a7b8c9d0e1f2a3b4c5d6a1b2c3"; +"User Delta" }
                            MenuItem { value = "e5f6a7b8c9d0e1f2a3b4c5d6a1b2c3d4"; +"User Epsilon" }
                        }
                    }

                    // Enterprise fields
                    if (contextType == "enterprise") {
                        Divider {}

                        Typography {
                            sx { fontWeight = web.cssom.integer(600); fontSize = 18.px }
                            +"Enterprise Options"
                        }

                        FormControl {
                            fullWidth = true

                            InputLabel { +"Subscription Tier" }
                            Select {
                                value = tier.unsafeCast<Nothing?>()
                                label = ReactNode("Subscription Tier")
                                onChange = { event, _ ->
                                    setTier(event.target.asDynamic().value as String)
                                }

                                MenuItem { value = "FREE"; +"Free" }
                                MenuItem { value = "STARTER"; +"Starter" }
                                MenuItem { value = "PROFESSIONAL"; +"Professional" }
                                MenuItem { value = "ENTERPRISE"; +"Enterprise" }
                            }
                        }

                        TextField {
                            fullWidth = true
                            label = ReactNode("Organization ID")
                            value = orgId
                            asDynamic().onChange = { event: dynamic ->
                                setOrgId(event.target.value as String)
                            }
                        }

                        TextField {
                            fullWidth = true
                            label = ReactNode("Employee Count")
                            asDynamic().type = "number"
                            value = employeeCount
                            asDynamic().onChange = { event: dynamic ->
                                setEmployeeCount(event.target.value as String)
                            }
                        }
                    }

                    // Evaluate button
                    Button {
                        variant = ButtonVariant.contained
                        size = Size.large
                        fullWidth = true
                        disabled = hotReload || loading
                        onClick = {
                            evaluateFeatures(
                                contextType, locale, platform, version, stableId,
                                tier, orgId, employeeCount,
                                { setResults(it) }, { setLoading(it) }, { setError(it) }
                            )
                        }

                        +"Evaluate Features"
                    }
                }
            }

            // Right column: Results
            Stack {
                sx { gap = 24.px }

                // Results panel
                Paper {
                    sx { padding = 24.px }

                    Typography {
                        sx { fontSize = 20.px; fontWeight = web.cssom.integer(600); marginTop = 0.px; marginBottom = 16.px }
                        +"📊 Feature Evaluation Results"
                    }

                    when {
                        loading -> {
                            Box {
                                sx { textAlign = TextAlign.center; padding = 32.px }
                                CircularProgress {}
                            }
                        }
                        error != null -> {
                            Alert {
                                asDynamic().severity = "error"
                                +error
                            }
                        }
                        results != null -> {
                            FeatureResultsGrid {
                                this.results = results
                                this.contextType = contextType
                            }
                        }
                        else -> {
                            Typography {
                                sx { textAlign = TextAlign.center; padding = 32.px; color = Color("#666") }
                                +"Click 'Evaluate Features' to see results"
                            }
                        }
                    }
                }

                // Rules info panel (load from window.KONDITIONAL_RULES)
                Paper {
                    sx { padding = 24.px }

                    Typography {
                        sx { fontSize = 20.px; fontWeight = web.cssom.integer(600); marginTop = 0.px; marginBottom = 16.px }
                        +"⚙️ Rules Configuration"
                    }

                    RulesInfoPanel {}
                }
            }
        }
    }
}

private fun evaluateFeatures(
    contextType: String,
    locale: String,
    platform: String,
    version: String,
    stableId: String,
    tier: String,
    orgId: String,
    employeeCount: String,
    setResults: (EvaluationResponse?) -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
) {
    MainScope().launch {
        try {
            setLoading(true)
            setError(null)

            val params = buildString {
                append("contextType=$contextType")
                append("&locale=$locale")
                append("&platform=$platform")
                append("&version=$version")
                append("&stableId=$stableId")

                if (contextType == "enterprise") {
                    append("&tier=$tier")
                    append("&orgId=$orgId")
                    append("&employeeCount=$employeeCount")
                }
            }

            val fetchOptions = json(
                "method" to "POST",
                "body" to params,
                "headers" to json(
                    "Content-Type" to "application/x-www-form-urlencoded"
                )
            )

            val response = window.fetch("/api/evaluate", fetchOptions.unsafeCast<RequestInit>()).await()

            if (response.ok) {
                val text = response.text().await()
                val data = Json.decodeFromString<EvaluationResponse>(text)
                setResults(data)
            } else {
                val errorText = response.text().await()
                setError("Evaluation failed: ${response.statusText} - $errorText")
            }
        } catch (e: Exception) {
            setError("Error: ${e.message}")
            console.error("Evaluation error:", e)
        } finally {
            setLoading(false)
        }
    }
}
