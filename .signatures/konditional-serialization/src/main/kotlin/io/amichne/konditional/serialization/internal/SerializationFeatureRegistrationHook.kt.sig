file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/internal/SerializationFeatureRegistrationHook.kt
package=io.amichne.konditional.serialization.internal
imports=io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.spi.FeatureRegistrationHook,io.amichne.konditional.serialization.FeatureRegistry
type=io.amichne.konditional.serialization.internal.SerializationFeatureRegistrationHook|kind=class|decl=class SerializationFeatureRegistrationHook : FeatureRegistrationHook
methods:
- override fun onFeatureDefined(feature: Feature<*, *, *>)
