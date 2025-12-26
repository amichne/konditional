package io.amichne.konditional.demo.evaluation

import kotlinx.browser.document
import react.create
import react.dom.client.createRoot
import web.dom.Element

/**
 * Entry point for Feature Evaluation React app
 */
object FeatureEvaluationClient {
    const val ROOT_ELEMENT_ID = "featureEvaluationRoot"

    fun init() {
        console.log("=== Initializing Feature Evaluation React App ===")

        val rootElement = document.getElementById(ROOT_ELEMENT_ID) as? Element
            ?: run {
                console.error("Root element #$ROOT_ELEMENT_ID not found")
                return
            }

        val root = createRoot(rootElement)
        root.render(FeatureEvaluationApp.create())

        console.log("Feature Evaluation App mounted successfully")
    }
}
