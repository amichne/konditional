file=konditional-core/src/main/kotlin/io/amichne/konditional/core/spi/FeatureRegistrationHooks.kt
package=io.amichne.konditional.core.spi
imports=io.amichne.konditional.core.features.Feature,java.util.ServiceLoader
type=io.amichne.konditional.core.spi.FeatureRegistrationHooks|kind=object|decl=internal object FeatureRegistrationHooks
fields:
- private val hooks: List<FeatureRegistrationHook> by lazy(LazyThreadSafetyMode.PUBLICATION)
methods:
- fun notifyFeatureDefined(feature: Feature<*, *, *>)
