package io.amichne.konditional.api

@ConsistentCopyVisibility
data class ShadowOptions internal constructor(
    val reportDecisionMismatches: Boolean,
    val evaluateCandidateWhenBaselineDisabled: Boolean,
    /**
     * Restricts shadow evaluation to the specified namespace ids.
     *
     * When `null` (the default), shadow evaluation is active for all namespaces.
     * When non-null, only namespaces whose id appears in this set participate;
     * all others return the baseline value immediately without evaluating the candidate.
     */
    val enabledForNamespaces: Set<String>?,
) {
    companion object {
        fun defaults(): ShadowOptions = ShadowOptions(
            reportDecisionMismatches = false,
            evaluateCandidateWhenBaselineDisabled = false,
            enabledForNamespaces = null,
        )

        fun of(
            reportDecisionMismatches: Boolean = false,
            evaluateCandidateWhenBaselineDisabled: Boolean = false,
            enabledForNamespaces: Set<String>? = null,
        ): ShadowOptions = ShadowOptions(
            reportDecisionMismatches = reportDecisionMismatches,
            evaluateCandidateWhenBaselineDisabled = evaluateCandidateWhenBaselineDisabled,
            enabledForNamespaces = enabledForNamespaces,
        )
    }
}
