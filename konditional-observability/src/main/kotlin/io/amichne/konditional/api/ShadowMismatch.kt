@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics

@ConsistentCopyVisibility
data class ShadowMismatch<T : Any> internal constructor(
    val featureKey: String,
    val baseline: EvaluationDiagnostics<T>,
    val candidate: EvaluationDiagnostics<T>,
    val kinds: Set<Kind>,
) {
    enum class Kind { VALUE, DECISION }
}
