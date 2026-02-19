file=konditional-otel/src/main/kotlin/io/amichne/konditional/otel/traces/TracingConfig.kt
package=io.amichne.konditional.otel.traces
imports=io.amichne.konditional.core.features.Feature
type=io.amichne.konditional.otel.traces.TracingConfig|kind=class|decl=data class TracingConfig( val enabled: Boolean = true, val samplingStrategy: SamplingStrategy = SamplingStrategy.PARENT_BASED, val includeContextAttributes: Boolean = true, val includeRuleDetails: Boolean = true, val sanitizePii: Boolean = true, )
type=io.amichne.konditional.otel.traces.SamplingStrategy|kind=interface|decl=sealed interface SamplingStrategy
type=io.amichne.konditional.otel.traces.ALWAYS|kind=object|decl=data object ALWAYS : SamplingStrategy
type=io.amichne.konditional.otel.traces.NEVER|kind=object|decl=data object NEVER : SamplingStrategy
type=io.amichne.konditional.otel.traces.PARENT_BASED|kind=object|decl=data object PARENT_BASED : SamplingStrategy
type=io.amichne.konditional.otel.traces.RATIO|kind=class|decl=data class RATIO(val percentage: Int) : SamplingStrategy
type=io.amichne.konditional.otel.traces.FEATURE_FILTER|kind=class|decl=data class FEATURE_FILTER(val predicate: (Feature<*, *, *>) -> Boolean) : SamplingStrategy
