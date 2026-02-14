file=konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt
package=io.amichne.konditional.serialization
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.values.FeatureId,java.util.concurrent.ConcurrentHashMap
type=io.amichne.konditional.serialization.FeatureRegistry|kind=object|decl=object FeatureRegistry
fields:
- private val registry
- private val defaultSamples
methods:
- fun <T : Any, C : Context> register(feature: Feature<T, C, *>)
- internal fun get(key: FeatureId): ParseResult<Feature<*, *, *>>
- internal fun contains(key: FeatureId): Boolean
- internal fun defaultSample(key: FeatureId): Any?
- fun clear()
