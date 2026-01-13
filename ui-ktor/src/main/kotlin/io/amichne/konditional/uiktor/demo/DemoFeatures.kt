package io.amichne.konditional.uiktor.demo

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.serialization.FeatureRegistry

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
 * Registers all demo features with the FeatureRegistry.
 * Must be called before deserializing demo snapshots.
 */
internal fun registerDemoFeatures() {
    FeatureRegistry.register(UiNamespace.dark_mode_enabled)
    FeatureRegistry.register(PaymentsNamespace.provider)
}
