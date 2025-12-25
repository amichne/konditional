package io.amichne.konditional.demo.ui.slices.tobebuilt

import kotlinx.html.a
import kotlinx.html.aside
import kotlinx.html.button
import kotlinx.html.checkBoxInput
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.label
import kotlinx.html.main
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.select
import org.w3c.dom.Element

object ToBeBuiltViewJs {
    fun render(root: Element, model: ToBeBuiltShellModel) {
        root.innerHTML = ""
        root.append {
            div {
                id = "tbbRoot"

                aside {
                    id = "tbbNav"

                    div { +model.title }

                    model.navItems.forEach { item ->
                        a(href = item.section.hrefPath) {
                            attributes["data-section"] = item.section.name
                            attributes["hx-get"] = item.section.fragmentPath
                            attributes["hx-target"] = "#tbbContent"
                            attributes["hx-swap"] = "outerHTML"
                            +item.section.label
                        }
                    }
                }

                div {
                    id = "tbbMain"

                    header {
                        id = "tbbTopBar"

                        button {
                            id = "tbbSidebarToggle"
                            +"Toggle sidebar"
                        }

                        label {
                            htmlFor = "tbbTheme"
                            +"Theme"
                        }
                        select {
                            id = "tbbTheme"
                            option { value = "system"; +"System" }
                            option { value = "dark"; +"Dark" }
                            option { value = "light"; +"Light" }
                        }

                        label {
                            htmlFor = "tbbDensity"
                            +"Density"
                        }
                        select {
                            id = "tbbDensity"
                            option { value = "comfortable"; +"Comfortable" }
                            option { value = "compact"; +"Compact" }
                        }

                        label {
                            htmlFor = "tbbReducedMotion"
                            +"Reduce motion"
                        }
                        checkBoxInput {
                            id = "tbbReducedMotion"
                        }
                    }

                    main {
                        id = "tbbContent"
                        attributes["hx-get"] = model.initialSection.fragmentPath
                        attributes["hx-trigger"] = "load"
                        attributes["hx-swap"] = "outerHTML"

                        section {
                            attributes["data-state"] = "loading"
                            p { +"Loading…" }
                        }
                    }
                }

                div {
                    id = "tbbToastHost"
                    attributes["aria-live"] = "polite"
                }
            }
        }
    }
}
