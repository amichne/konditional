package io.amichne.konditional.api

@ConsistentCopyVisibility
data class ShadowOptions internal constructor(
    val reportDecisionMismatches: Boolean,
    val evaluateCandidateWhenBaselineDisabled: Boolean,
) {
    companion object {
        fun defaults(): ShadowOptions = ShadowOptions(
            reportDecisionMismatches = false,
            evaluateCandidateWhenBaselineDisabled = false,
        )

        fun of(
            reportDecisionMismatches: Boolean = false,
            evaluateCandidateWhenBaselineDisabled: Boolean = false,
        ): ShadowOptions = ShadowOptions(
            reportDecisionMismatches = reportDecisionMismatches,
            evaluateCandidateWhenBaselineDisabled = evaluateCandidateWhenBaselineDisabled,
        )
    }
}
