package io.amichne.konditional.api

import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Result of a feature evaluation that is suitable for operational debugging.
 *
 * This is intentionally a "flattened" representation: it captures the observable
 * constraints and precedence of the chosen rule without exposing internal model
 * types like ConditionalValue or BaseEvaluable.
 */
@ConsistentCopyVisibility
data class EvaluationResult<T : Any> internal constructor(
    val namespaceId: String,
    val featureKey: String,
    val configVersion: String?,
    val mode: Metrics.Evaluation.EvaluationMode,
    val durationNanos: Long,
    val value: T,
    val decision: Decision,
) {
    sealed interface Decision {
        data object RegistryDisabled : Decision
        data object Inactive : Decision

        /**
         * A rule was applied and produced the returned [EvaluationResult.value].
         *
         * @property matched The rule that produced the value.
         * @property skippedByRollout A more specific rule that matched by criteria but excluded this context by rampUp.
         */
        @ConsistentCopyVisibility
        data class Rule internal constructor(
            val matched: RuleMatch,
            val skippedByRollout: RuleMatch? = null,
        ) : Decision

        /**
         * No rule produced a value; the declared default was returned.
         *
         * @property skippedByRollout The most specific rule that matched by criteria but excluded this context by rampUp.
         */
        @ConsistentCopyVisibility
        data class Default internal constructor(
            val skippedByRollout: RuleMatch? = null,
        ) : Decision
    }

    @ConsistentCopyVisibility
    data class RuleMatch internal constructor(
        val rule: RuleExplanation,
        val bucket: BucketInfo,
    )

    @ConsistentCopyVisibility
    data class RuleExplanation internal constructor(
        val note: String?,
        val rollout: RampUp,
        val locales: Set<String>,
        val platforms: Set<String>,
        val versionRange: VersionRange,
        val axes: Map<String, Set<String>>,
        val baseSpecificity: Int,
        val extensionSpecificity: Int,
        val totalSpecificity: Int,
        val extensionClassName: String?,
    )
}
