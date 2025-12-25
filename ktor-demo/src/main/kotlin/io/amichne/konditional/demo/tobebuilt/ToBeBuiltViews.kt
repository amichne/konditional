package io.amichne.konditional.demo.tobebuilt

import kotlinx.html.A
import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.HEAD
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.textArea
import kotlinx.html.title

internal fun HTML.renderToBeBuiltShell(page: ToBeBuiltPage, content: FlowContent.() -> Unit) {
    head {
        renderToBeBuiltHead(titleText = page.title)
    }
    body {
        renderToBeBuiltBody(page, content)
    }
}

private fun HEAD.renderToBeBuiltHead(titleText: String) {
    meta(charset = "utf-8")
    meta(name = "viewport", content = "width=device-width, initial-scale=1")
    title { +titleText }
    link(rel = "stylesheet", href = "/static/to-be-built/to-be-built.css")
    script(src = "/static/htmx.min.js") { defer = true }
    script(src = "/static/demo-client.js") { defer = true }
}

private fun BODY.renderToBeBuiltBody(
    page: ToBeBuiltPage,
    content: FlowContent.() -> Unit,
) {
    attributes["data-sidebar"] = "expanded"
    div(classes = "tbb-root") {
        id = "tbbRoot"

        asideNav(activePrefix = page.activeNavPrefix)

        div(classes = "tbb-main") {
            topBar(titleText = page.title)
            div(classes = "tbb-toasts") { id = "tbbToastHost" }
            div(classes = "tbb-content") {
                id = "tbbContent"
                content()
            }
        }
    }
}

private fun FlowContent.asideNav(activePrefix: String) {
    div(classes = "tbb-sidebar") {
        div(classes = "tbb-sidebarHeader") {
            a(href = "/to-be-built/", classes = "tbb-logo") { +"ConfigUI" }
            button(classes = "tbb-button tbb-buttonGhost") {
                id = "tbbSidebarToggle"
                type = kotlinx.html.ButtonType.button
                +"Collapse"
            }
        }

        nav(classes = "tbb-nav") {
            navSection(
                title = "Design Primitives",
                active = activePrefix.startsWith("/to-be-built/primitives"),
                items = listOf(
                    "Colors" to "/to-be-built/primitives/colors",
                    "Typography" to "/to-be-built/primitives/typography",
                    "Spacing" to "/to-be-built/primitives/spacing",
                    "Motion" to "/to-be-built/primitives/motion",
                ),
            )
            navSection(
                title = "Core Components",
                active = activePrefix.startsWith("/to-be-built/components"),
                items = listOf(
                    "Inputs" to "/to-be-built/components/inputs",
                    "Layout" to "/to-be-built/components/layout",
                    "Navigation" to "/to-be-built/components/navigation",
                    "Overlays" to "/to-be-built/components/overlays",
                    "Data Display" to "/to-be-built/components/data-display",
                ),
            )
            navSection(
                title = "Patterns & Recipes",
                active = activePrefix.startsWith("/to-be-built/patterns"),
                items = listOf(
                    "Schema Forms" to "/to-be-built/patterns/schema-forms",
                    "Safe Publishing" to "/to-be-built/patterns/safe-publishing",
                    "Large Datasets" to "/to-be-built/patterns/large-datasets",
                ),
            )
            navLink(label = "Playground", href = "/to-be-built/playground", active = activePrefix.startsWith("/to-be-built/playground"))
            navLink(label = "Config Demo", href = "/to-be-built/demo", active = activePrefix.startsWith("/to-be-built/demo"))
        }
    }
}

private fun FlowContent.topBar(titleText: String) {
    header(classes = "tbb-topbar") {
        div(classes = "tbb-topbarLeft") {
            div(classes = "tbb-title") { +titleText }
        }
        div(classes = "tbb-topbarRight") {
            fieldSelect(
                id = "tbbTheme",
                labelText = "Theme",
                options = listOf(
                    "dark" to "Dark",
                    "light" to "Light",
                    "system" to "System",
                ),
            )
            fieldSelect(
                id = "tbbDensity",
                labelText = "Density",
                options = listOf(
                    "comfortable" to "Comfortable",
                    "compact" to "Compact",
                ),
            )
            label(classes = "tbb-field tbb-fieldInline") {
                input(type = kotlinx.html.InputType.checkBox) {
                    id = "tbbReducedMotion"
                }
                span { +"Reduced motion" }
            }
        }
    }
}

private fun FlowContent.fieldSelect(
    id: String,
    labelText: String,
    options: List<Pair<String, String>>,
) {
    label(classes = "tbb-field") {
        span { +labelText }
        select {
            this.id = id
            options.forEach { (value, label) ->
                option {
                    this.value = value
                    +label
                }
            }
        }
    }
}

private fun FlowContent.navSection(
    title: String,
    active: Boolean,
    items: List<Pair<String, String>>,
) {
    div(classes = "tbb-navSection") {
        div(classes = "tbb-navSectionTitle") {
            attributes["data-active"] = active.toString()
            +title
        }
        div(classes = "tbb-navSectionItems") {
            items.forEach { (label, href) ->
                navLink(label, href, active = false)
            }
        }
    }
}

private fun FlowContent.navLink(label: String, href: String, active: Boolean) {
    a(classes = "tbb-navLink") {
        toBeBuiltHxLink(href)
        attributes["data-active"] = active.toString()
        +label
    }
}

private fun A.toBeBuiltHxLink(hrefValue: String) {
    href = hrefValue
    attributes["hx-get"] = hrefValue
    attributes["hx-target"] = "#tbbContent"
    attributes["hx-swap"] = "outerHTML"
    attributes["hx-push-url"] = "true"
}

internal fun FlowContent.renderPageContent(page: ToBeBuiltPage) {
    when (page) {
        is ToBeBuiltPage.Index -> renderIndex()
        is ToBeBuiltPage.PrimitivesColors -> renderColors()
        is ToBeBuiltPage.PrimitivesTypography -> renderTypography()
        is ToBeBuiltPage.ComponentsInputs -> renderInputs(apiTimeoutMs = InputsDefaults.apiTimeoutMs)
        is ToBeBuiltPage.PatternsSchemaForms -> renderSchemaForms(SchemaFormsModel.EMPTY)
        is ToBeBuiltPage.Placeholder -> renderPlaceholder(page)
        is ToBeBuiltPage.NotFound -> renderNotFound(page)
    }
}

internal fun FlowContent.renderPageContent(
    page: ToBeBuiltPage,
    state: ToBeBuiltRenderState,
) {
    when (page) {
        is ToBeBuiltPage.Index -> renderIndex()
        is ToBeBuiltPage.PrimitivesColors -> renderColors()
        is ToBeBuiltPage.PrimitivesTypography -> renderTypography()
        is ToBeBuiltPage.ComponentsInputs -> renderInputs(apiTimeoutMs = state.inputsApiTimeoutMs)
        is ToBeBuiltPage.PatternsSchemaForms -> renderSchemaForms(state.schemaForms)
        is ToBeBuiltPage.Placeholder -> renderPlaceholder(page)
        is ToBeBuiltPage.NotFound -> renderNotFound(page)
    }
}

private fun FlowContent.renderIndex() {
    div(classes = "tbb-stack") {
        h1(classes = "tbb-h1") { +"To Be Built" }
        p(classes = "tbb-muted") {
            +"A Kotlin-first replacement for the React prototype shell, using HTMX for navigation and partial swaps."
        }
    }

    div(classes = "tbb-grid") {
        cardLink(
            title = "Design Primitives",
            description = "Colors, typography, spacing, and motion tokens.",
            href = "/to-be-built/primitives/colors",
        )
        cardLink(
            title = "Core Components",
            description = "Inputs and form building blocks for config workflows.",
            href = "/to-be-built/components/inputs",
        )
        cardLink(
            title = "Patterns & Recipes",
            description = "Schema-driven editing and publishing flows.",
            href = "/to-be-built/patterns/schema-forms",
        )
        cardLink(
            title = "Config Demo",
            description = "Jump into the existing Konditional demo + editor.",
            href = "/to-be-built/demo",
        )
    }
}

private fun FlowContent.cardLink(title: String, description: String, href: String) {
    a(classes = "tbb-card") {
        toBeBuiltHxLink(href)
        h3(classes = "tbb-h3") { +title }
        p(classes = "tbb-muted") { +description }
    }
}

private fun FlowContent.renderColors() {
    h1(classes = "tbb-h1") { +"Colors" }
    p(classes = "tbb-muted") { +"Semantic tokens and neutral surfaces (visual parity intentionally simplified)." }

    div(classes = "tbb-grid") {
        listOf(
            "Primary" to "tbb-swatchPrimary",
            "Accent" to "tbb-swatchAccent",
            "Success" to "tbb-swatchSuccess",
            "Warning" to "tbb-swatchWarning",
            "Error" to "tbb-swatchError",
            "Info" to "tbb-swatchInfo",
        ).forEach { (name, swatchClass) ->
            div(classes = "tbb-card") {
                div(classes = "tbb-swatch $swatchClass") {}
                h3(classes = "tbb-h3") { +name }
                p(classes = "tbb-muted") { +"Token: $name" }
            }
        }
    }
}

private fun FlowContent.renderTypography() {
    h1(classes = "tbb-h1") { +"Typography" }
    p(classes = "tbb-muted") { +"Heading scale + body/caption variants." }

    div(classes = "tbb-stack") {
        h1(classes = "tbb-h1") { +"Heading 1" }
        h2(classes = "tbb-h2") { +"Heading 2" }
        h3(classes = "tbb-h3") { +"Heading 3" }
        p { +"Body text for general content throughout the application." }
        p(classes = "tbb-muted") { +"Caption / muted foreground text." }
    }
}

private fun FlowContent.renderInputs(apiTimeoutMs: Int) {
    h1(classes = "tbb-h1") { +"Inputs & Forms" }
    p(classes = "tbb-muted") { +"Native controls rendered server-side; behavior is intentionally minimal." }

    div(classes = "tbb-formGrid") {
        fieldText(id = "tbbInputBasic", labelText = "Basic Input", placeholder = "Enter text…")
        fieldText(id = "tbbInputDisabled", labelText = "Disabled Input", placeholder = "Disabled…", disabled = true)
        fieldSelect(
            id = "tbbSelectLogLevel",
            labelText = "Log Level",
            options = listOf("debug" to "Debug", "info" to "Info", "warn" to "Warning", "error" to "Error"),
        )
        fieldTextArea(id = "tbbTextarea", labelText = "Textarea", placeholder = "Enter description…")
        fieldCheckbox(id = "tbbCheck1", labelText = "Enable feature A")
        fieldCheckbox(id = "tbbCheck2", labelText = "Enable feature B (default checked)", checked = true)
        fieldCheckbox(id = "tbbCheck3", labelText = "Disabled option", disabled = true)
        renderInputsSliderCard(apiTimeoutMs = apiTimeoutMs, errorMessage = null)
    }
}

private fun FlowContent.renderSchemaForms(model: SchemaFormsModel) {
    h1(classes = "tbb-h1") { +"Schema-Driven Forms" }
    p(classes = "tbb-muted") { +"Read-only flag browser backed by the current Konditional snapshot." }

    div(classes = "tbb-grid") {
        id = "tbbSchemaForms"

        div(classes = "tbb-card") {
            h2(classes = "tbb-h2") { +"Flags" }

            if (model.flagsByNamespace.isEmpty()) {
                p(classes = "tbb-muted") { +"No flags available (feature containers not initialized?)" }
            } else {
                model.flagsByNamespace.forEach { (namespace, flags) ->
                    div(classes = "tbb-muted") { +namespace }
                    flags.forEach { flag ->
                        button(classes = "tbb-button tbb-buttonGhost") {
                            type = kotlinx.html.ButtonType.button
                            attributes["hx-get"] = "/to-be-built/patterns/schema-forms/flag?key=${flag.encodedKey}"
                            attributes["hx-target"] = "#tbbSchemaPanel"
                            attributes["hx-swap"] = "outerHTML"
                            div { +flag.shortKey }
                            div(classes = "tbb-muted") {
                                val activeText = if (flag.isActive) "Active" else "Inactive"
                                +("${flag.type} • ${flag.rulesCount} rule(s) • $activeText")
                            }
                        }
                    }
                }
            }
        }

        div(classes = "tbb-card") {
            id = "tbbSchemaPanel"
            h2(classes = "tbb-h2") { +"Details" }
            p(classes = "tbb-muted") { +"Select a flag to view its JSON." }
        }
    }
}

private fun FlowContent.renderPlaceholder(page: ToBeBuiltPage.Placeholder) {
    h1(classes = "tbb-h1") { +page.title }
    p(classes = "tbb-muted") { +page.description }
    p(classes = "tbb-muted") { +"This section exists to preserve route parity with the React prototype." }

    if (page.title == "Config Demo") {
        div(classes = "tbb-stack") {
            a(classes = "tbb-button") {
                href = "/"
                +"Open Feature Evaluation Demo"
            }
            a(classes = "tbb-button tbb-buttonGhost") {
                href = "/config-state"
                +"Open Configuration State Editor"
            }
        }
    }
}

private fun FlowContent.renderNotFound(page: ToBeBuiltPage.NotFound) {
    h1(classes = "tbb-h1") { +"404" }
    p(classes = "tbb-muted") { +"No route for: ${page.requestedPath}" }
    a(classes = "tbb-button") {
        toBeBuiltHxLink("/to-be-built/")
        +"Return to Home"
    }
}

private fun FlowContent.fieldText(
    id: String,
    labelText: String,
    placeholder: String,
    disabled: Boolean = false,
) {
    label(classes = "tbb-fieldBlock") {
        span(classes = "tbb-label") { +labelText }
        input(type = kotlinx.html.InputType.text, classes = "tbb-input") {
            this.id = id
            this.placeholder = placeholder
            this.disabled = disabled
        }
    }
}

private fun FlowContent.fieldTextArea(
    id: String,
    labelText: String,
    placeholder: String,
) {
    label(classes = "tbb-fieldBlock") {
        span(classes = "tbb-label") { +labelText }
        textArea(classes = "tbb-input") {
            this.id = id
            attributes["placeholder"] = placeholder
            attributes["rows"] = "3"
        }
    }
}

private fun FlowContent.fieldCheckbox(
    id: String,
    labelText: String,
    checked: Boolean = false,
    disabled: Boolean = false,
) {
    label(classes = "tbb-fieldInline") {
        input(type = kotlinx.html.InputType.checkBox) {
            this.id = id
            this.checked = checked
            this.disabled = disabled
        }
        span(classes = "tbb-label") { +labelText }
    }
}

internal fun FlowContent.renderInputsSliderCard(
    apiTimeoutMs: Int,
    errorMessage: String?,
) {
    div(classes = "tbb-fieldBlock") {
        id = "tbbInputsSliderCard"
        renderInputsSliderCardBody(apiTimeoutMs = apiTimeoutMs, errorMessage = errorMessage)
    }
}

internal fun FlowContent.renderInputsSliderCardBody(
    apiTimeoutMs: Int,
    errorMessage: String?,
) {
    span(classes = "tbb-label") { +"API Timeout (ms)" }

    div(classes = "tbb-muted") { +"Current: $apiTimeoutMs ms" }

    input(type = kotlinx.html.InputType.range, classes = "tbb-input") {
        id = "tbbRange"
        name = "apiTimeoutMs"
        min = InputsDefaults.apiTimeoutMinMs.toString()
        max = InputsDefaults.apiTimeoutMaxMs.toString()
        step = InputsDefaults.apiTimeoutStepMs.toString()
        value = apiTimeoutMs.toString()

        attributes["hx-post"] = "/to-be-built/components/inputs/slider"
        attributes["hx-trigger"] = "input changed delay:150ms"
        attributes["hx-target"] = "#tbbInputsSliderCard"
        attributes["hx-swap"] = "outerHTML"
    }

    p(classes = "tbb-muted") { +"${InputsDefaults.apiTimeoutMinMs}ms → ${InputsDefaults.apiTimeoutMaxMs}ms" }

    if (!errorMessage.isNullOrBlank()) {
        p(classes = "tbb-muted") { +"Error: $errorMessage" }
    }
}
