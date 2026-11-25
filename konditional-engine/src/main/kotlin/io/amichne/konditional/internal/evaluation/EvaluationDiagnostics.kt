@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.evaluation

import io.amichne.konditional.api.BucketInfo
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.values.RuleId

/**
 * Internal diagnostics snapshot for a single evaluation.
 *
 * Invariants:
 * - Captures the outcome from exactly one deterministic evaluation
 *   against one immutable registry snapshot.
 * - Contains only trusted, fully computed data produced by core evaluation;
 *   no parsing or lazy rule inspection occurs here.
 * - `duration` reflects the measured evaluation wall-clock duration
 *   for observability only and does not affect semantics.
 *
 * Boundary expectations:
 * - The snapshot is produced by Konditional internals after rule resolution
 *   and bucketing are complete.
 * - Consumers may safely read this model from internal APIs and observability
 *   integrations without additional casting of rule details beyond the
 *   [`Decision`] branch they are handling.
 *
 * Error semantics:
 * - Construction does not perform recovery or validation.
 *   Invalid intermediate state is prevented by the evaluation pipeline.
 *
 * Performance notes:
 * - This model is immutable and allocation-only.
 *   It is intended to be cheap to copy and safe to share across observers.
 */
data class EvaluationDiagnostics<T : Any>(
    val namespaceId: String,
    val featureKey: String,
    val configVersion: String?,
    val mode: Metrics.Evaluation.EvaluationMode,
    val durationNanos: Long,
    val value: T,
    val decision: Decision,
) {
    /**
     * Closed set of deterministic evaluation outcomes.
     *
     * Invariants:
     * - Exactly one branch describes the final evaluation path.
     * - Rule-specific diagnostics are present only for [`Rule`] and rollout-skipped [`Default`] decisions.
     *
     * Performance notes:
     * - Branch inspection is allocation-free after the enclosing snapshot is created.
     */
    sealed interface Decision {
        /**
         * Indicates evaluation returned the feature default because the namespace registry was globally disabled.
         *
         * Guarantees:
         * - No rule matching or bucketing contributed to the returned value.
         */
        data object RegistryDisabled : Decision

        /**
         * Indicates evaluation returned the feature default because the flag definition was inactive.
         *
         * Guarantees:
         * - No rule matching or bucketing contributed to the returned value.
         */
        data object Inactive : Decision

        /**
         * Indicates evaluation resolved a rule-backed value.
         *
         * Invariants:
         * - [matched] contains the winning rule explanation and its computed bucket metadata.
         * - [skippedByRollout], when present, identifies the first matching rule excluded by rollout before the winner.
         */
        data class Rule(
            val matched: RuleMatch<RuleExplanation>,
            val skippedByRollout: RuleMatch<RuleExplanation>? = null,
        ) : Decision

        /**
         * Indicates evaluation returned the flag default because no eligible rule produced a value.
         *
         * Invariants:
         * - [skippedByRollout], when present, identifies the first rule
         *   that matched criteria but was excluded by rollout.
         */
        data class Default(
            val skippedByRollout: RuleMatch<RuleExplanation>? = null,
        ) : Decision
    }

    /**
     * Typed rule-match diagnostics paired with bucket metadata.
     *
     * `RuleMatch` is generic so consumers can work with the concrete rule details type directly while still sharing a
     * common match wrapper. The wrapped rule data is also exposed on this type via interface delegation, allowing
     * callers to read properties such as [note] or [totalSpecificity] without navigating through [rule].
     *
     * Invariants:
     * - [rule] and [bucket] describe the same evaluation attempt.
     * - [bucket] is always the computed deterministic bucket used for the associated rule decision.
     *
     * Performance notes:
     * - Delegation avoids copying rule details; the wrapper only stores the typed rule payload and bucket metadata.
     */
    data class RuleMatch<out D : RuleDetails>(
        val rule: D,
        val bucket: BucketInfo,
    ) : RuleDetails by rule

    /**
     * Contract for typed rule details exposed through [`RuleMatch`].
     *
     * Invariants:
     * - Implementations must be immutable and fully derived from trusted evaluation definitions.
     * - Property values must be deterministic for a fixed rule definition.
     *
     * Performance notes:
     * - The interface is property-only so callers can inspect rule details without additional computation.
     */
    sealed interface RuleDetails {
        val note: String?
        val rollout: Rollout
        val targeting: TargetingNode
        val totalSpecificity: Int
        val ruleId: RuleId
    }

    /** Structural explanation of the rule's criteria. Kotlin predicates remain opaque. */
    sealed interface TargetingNode {
        data class All(val children: List<TargetingNode>) : TargetingNode
        data class AnyOf(val children: List<TargetingNode>) : TargetingNode
        data class Facts(val expression: FactSpec) : TargetingNode
        data object Custom : TargetingNode
        data class Guarded(val child: TargetingNode) : TargetingNode
    }

    /** Structural criteria and identity of the winning or rollout-excluded rule. */
    data class RuleExplanation(
        override val note: String?,
        override val rollout: Rollout,
        override val targeting: TargetingNode,
        override val totalSpecificity: Int,
        override val ruleId: RuleId,
    ) : RuleDetails
}
