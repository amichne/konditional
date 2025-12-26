package io.amichne.konditional.configstate.ui

import kotlinx.browser.document
import kotlin.js.unsafeCast
import react.create
import react.dom.client.createRoot
import web.dom.Element

object ConfigStateCatalogClient {
    fun init() {
        val rootElement = document.getElementById(ROOT_ELEMENT_ID) ?: return
        createRoot(rootElement.unsafeCast<Element>()).render(ConfigStateCatalogApp.create())
    }

    const val ROOT_ELEMENT_ID: String = "configstateCatalogRoot"
}
