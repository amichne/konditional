package io.amichne.konditional.api

@ConsistentCopyVisibility
data class ShadowMismatch<T : Any> internal constructor(
    val featureKey: String,
    val baseline: EvaluationResult<T>,
    val candidate: EvaluationResult<T>,
    val kinds: Set<Kind>,
) {
    enum class Kind { VALUE, DECISION }
}
