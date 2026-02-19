file=konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowMismatch.kt
package=io.amichne.konditional.api
imports=io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
type=io.amichne.konditional.api.ShadowMismatch|kind=class|decl=data class ShadowMismatch<T : Any> internal constructor( val featureKey: String, val baseline: EvaluationDiagnostics<T>, val candidate: EvaluationDiagnostics<T>, val kinds: Set<Kind>, )
type=io.amichne.konditional.api.Kind|kind=enum|decl=enum class Kind
