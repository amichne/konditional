package io.amichne.konditional.uiktor.demo

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.runtime.load

/**
 * Demo feature namespaces for UI demonstrations.
 * These features are unrelated to production code and exist solely for demo purposes.
 */
internal object UiNamespace : Namespace("ui") {
    val dark_mode_enabled by boolean<Context>(default = false)
}

internal object PaymentsNamespace : Namespace("payments") {
    val provider by string<Context>(default = "STRIPE")
}

/**
 * Initializes demo features so serialization can resolve them.
 * Must be called before deserializing demo snapshots.
 */
internal fun registerDemoFeatures() {
    UiNamespace.dark_mode_enabled
    PaymentsNamespace.provider
}
