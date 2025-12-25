package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.configstate.Json.stableString
import io.amichne.konditional.demo.client.configstate.HtmlLayout.div
import org.w3c.dom.HTMLDivElement

object Sidebar {
    internal fun renderFlagList(
        layout: Layout,
        state: LoadedState,
    ) {
        layout.flagList.innerHTML = ""

        val flags = state.draftSnapshot.flags.unsafeCast<Array<dynamic>>()
        val filter = state.flagFilter.trim().lowercase()
        val matches =
            flags.withIndex().filter { (_, flag) ->
                filter.isBlank() || stableString(flag.key).lowercase().contains(filter)
            }

        layout.flagCount.textContent = "${matches.size} / ${flags.size}"

        matches.forEach { (index, flag) ->
            val item = div("flag-item")
            item.addEventListener("click", { _ ->
                Client.updateState { it.copy(selectedFlagIndex = index) }
                Client.render(layout)
            })

            val label = div("flag-label")
            label.textContent = stableString(flag.key)
            item.appendChild(label)

            val meta = div("flag-meta")
            val isActive = flag.isActive as? Boolean ?: true
            meta.appendChild(
                pill(
                    if (isActive) "Active" else "Inactive",
                    if (isActive) "pill pill-active" else "pill pill-inactive"
                )
            )

            val valueType = stableString(flag.defaultValue?.type ?: "")
            if (valueType.isNotBlank()) {
                meta.appendChild(pill(valueType, "pill pill-type"))
            }

            val rulesCount = (flag.rules as? Array<*>)?.size ?: 0
            meta.appendChild(pill("$rulesCount rules", "pill"))
            item.appendChild(meta)

            if (index == state.selectedFlagIndex) {
                item.classList.add("selected")
            }
            layout.flagList.appendChild(item)
        }

        layout.searchInput.value = state.flagFilter
        layout.searchInput.oninput = { _ ->
            Client.updateState { it.copy(flagFilter = layout.searchInput.value) }
            Client.render(layout)
            null
        }
    }

    private fun pill(text: String, classes: String): HTMLDivElement =
        div(classes).also { it.textContent = text }
}
