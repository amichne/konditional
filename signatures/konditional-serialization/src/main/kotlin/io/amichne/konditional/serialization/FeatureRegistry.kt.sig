file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseFailure,io.amichne.konditional.values.FeatureId,java.util.concurrent.ConcurrentHashMap
type=io.amichne.konditional.serialization.FeatureRegistry|kind=object|decl=object FeatureRegistry
methods:
- fun <T : Any, C : Context> register(feature: Feature<T, C, *>)
- fun clear()
