package io.amichne.konditional.demo.client.tobebuilt

import io.amichne.konditional.demo.ui.slices.tobebuilt.ToBeBuiltNavItem
import io.amichne.konditional.demo.ui.slices.tobebuilt.ToBeBuiltSection
import io.amichne.konditional.demo.ui.slices.tobebuilt.ToBeBuiltShellModel
import io.amichne.konditional.demo.ui.slices.tobebuilt.ToBeBuiltViewJs
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement

object ToBeBuiltClient {
    private const val themeKey = "tbb.theme"
    private const val densityKey = "tbb.density"
    private const val reducedMotionKey = "tbb.reducedMotion"
    private const val sidebarKey = "tbb.sidebar"

    fun init() {
        document.addEventListener("DOMContentLoaded", {
            val mount = document.getElementById("tbbMount") as? HTMLElement
            mount?.let(::ensureShellRendered)

            document.getElementById("tbbRoot")
                ?.unsafeCast<HTMLElement>()
                ?.let(::bind)
        })
    }

    private fun ensureShellRendered(mount: HTMLElement) {
        if (document.getElementById("tbbRoot") == null) {
            val initialSection = readInitialSection() ?: ToBeBuiltSection.INDEX

            ToBeBuiltViewJs.render(
                root = mount,
                model = ToBeBuiltShellModel(
                    title = "To Be Built",
                    navItems = ToBeBuiltSection.entries.map(::ToBeBuiltNavItem),
                    initialSection = initialSection,
                ),
            )
        }
    }

    private fun readInitialSection(): ToBeBuiltSection? {
        val raw =
            run {
                val w: dynamic = window.asDynamic()
                val anyValue = w.TBB_INITIAL_SECTION
                anyValue as? String
            }

        return ToBeBuiltSection.entries.firstOrNull { it.name == raw }
    }

    private fun bind(root: HTMLElement) {
        val prefs = loadPrefs()
        applyPrefs(document, prefs)
        syncControlsFromPrefs(document, prefs)
        bindControls(document)
        bindSidebarToggle(root, prefs)
        bindToastHost()
    }

    private fun bindControls(document: Document) {
        document.getElementById("tbbTheme")
            ?.unsafeCast<HTMLSelectElement>()
            ?.addEventListener("change", { event ->
                val value = (event.target as? HTMLSelectElement)?.value ?: return@addEventListener
                val theme = Theme.parse(value)
                save(themeKey, theme.storageValue)
                applyTheme(document, theme)
            })

        document.getElementById("tbbDensity")
            ?.unsafeCast<HTMLSelectElement>()
            ?.addEventListener("change", { event ->
                val value = (event.target as? HTMLSelectElement)?.value ?: return@addEventListener
                val density = Density.parse(value)
                save(densityKey, density.storageValue)
                applyDensity(document, density)
            })

        document.getElementById("tbbReducedMotion")
            ?.unsafeCast<HTMLInputElement>()
            ?.addEventListener("change", { event ->
                val checked = (event.target as? HTMLInputElement)?.checked ?: return@addEventListener
                save(reducedMotionKey, checked.toString())
                applyReducedMotion(document, checked)
            })
    }

    private fun bindSidebarToggle(root: HTMLElement, prefs: Prefs) {
        var sidebar = prefs.sidebar
        document.getElementById("tbbSidebarToggle")
            ?.unsafeCast<HTMLElement>()
            ?.addEventListener("click", { _ ->
                sidebar = sidebar.toggled()
                save(sidebarKey, sidebar.storageValue)
                applySidebar(sidebar)
            })
    }

    private fun applyPrefs(document: Document, prefs: Prefs) {
        applyTheme(document, prefs.theme)
        applyDensity(document, prefs.density)
        applyReducedMotion(document, prefs.reducedMotion)
        applySidebar(prefs.sidebar)
    }

    private fun syncControlsFromPrefs(document: Document, prefs: Prefs) {
        document.getElementById("tbbTheme")
            ?.unsafeCast<HTMLSelectElement>()
            ?.let { it.value = prefs.theme.storageValue }

        document.getElementById("tbbDensity")
            ?.unsafeCast<HTMLSelectElement>()
            ?.let { it.value = prefs.density.storageValue }

        document.getElementById("tbbReducedMotion")
            ?.unsafeCast<HTMLInputElement>()
            ?.let { it.checked = prefs.reducedMotion }
    }

    private fun loadPrefs(): Prefs =
        Prefs(
            theme = Theme.parse(load(themeKey)),
            density = Density.parse(load(densityKey)),
            reducedMotion = load(reducedMotionKey).toBooleanStrictOrNull() ?: false,
            sidebar = Sidebar.parse(load(sidebarKey)),
        )

    private fun applyTheme(document: Document, theme: Theme) {
        val root = document.documentElement ?: return
        val systemDark = window.matchMedia("(prefers-color-scheme: dark)").matches
        val isDark = theme == Theme.Dark || (theme == Theme.System && systemDark)

        if (isDark) {
            root.classList.add("dark")
        } else {
            root.classList.remove("dark")
        }
    }

    private fun applyDensity(document: Document, density: Density) {
        val root = document.documentElement ?: return
        root.classList.remove("density-comfortable", "density-compact")
        root.classList.add(density.cssClass)
    }

    private fun applyReducedMotion(document: Document, reduced: Boolean) {
        val root = document.documentElement ?: return
        if (reduced) {
            root.classList.add("reduce-motion")
        } else {
            root.classList.remove("reduce-motion")
        }
    }

    private fun applySidebar(sidebar: Sidebar) {
        document.body?.setAttribute("data-sidebar", sidebar.storageValue)
    }

    private fun bindToastHost() {
        val host = document.getElementById("tbbToastHost") as? HTMLElement
        if (host != null) {
            document.body?.addEventListener("toast", { event ->
                val detail = event.asDynamic().detail
                val message = detail?.message as? String

                if (!message.isNullOrBlank()) {
                    host.textContent = message
                    host.setAttribute("data-visible", "true")

                    window.setTimeout(
                        {
                            host.removeAttribute("data-visible")
                            host.textContent = ""
                        },
                        2500,
                    )
                }
            })
        }
    }

    private fun load(key: String): String =
        window.localStorage.getItem(key) ?: ""

    private fun save(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    private data class Prefs(
        val theme: Theme,
        val density: Density,
        val reducedMotion: Boolean,
        val sidebar: Sidebar,
    )

    private enum class Theme(val storageValue: String) {
        Light("light"),
        Dark("dark"),
        System("system"),
        ;

        companion object {
            fun parse(value: String): Theme =
                when (value) {
                    Light.storageValue -> Light
                    System.storageValue -> System
                    else -> Dark
                }
        }
    }

    private enum class Density(val storageValue: String, val cssClass: String) {
        Comfortable("comfortable", "density-comfortable"),
        Compact("compact", "density-compact"),
        ;

        companion object {
            fun parse(value: String): Density =
                when (value) {
                    Compact.storageValue -> Compact
                    else -> Comfortable
                }
        }
    }

    private enum class Sidebar(val storageValue: String) {
        Expanded("expanded"),
        Collapsed("collapsed"),
        ;

        fun toggled(): Sidebar =
            when (this) {
                Expanded -> Collapsed
                Collapsed -> Expanded
            }

        companion object {
            fun parse(value: String): Sidebar =
                when (value) {
                    Collapsed.storageValue -> Collapsed
                    else -> Expanded
                }
        }
    }
}
