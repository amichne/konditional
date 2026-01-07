package io.amichne.konditional.serialization.internal

import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.spi.FeatureRegistrationHook
import io.amichne.konditional.serialization.FeatureRegistry

/**
 * Registers feature definitions into [FeatureRegistry] when `:konditional-serialization` is present.
 *
 * Discovered via [java.util.ServiceLoader] through the `:konditional-core` SPI.
 */
class SerializationFeatureRegistrationHook : FeatureRegistrationHook {
    override fun onFeatureDefined(feature: Feature<*, *, *>) {
        FeatureRegistry.register(feature)
    }
}
