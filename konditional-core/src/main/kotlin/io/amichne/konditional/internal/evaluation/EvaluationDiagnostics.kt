@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.evaluation

import io.amichne.konditional.api.BucketInfo
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Internal diagnostics snapshot for a single evaluation.
 *
 * This is intentionally not part of the consumer-facing API. Sibling modules (runtime/openfeature/observability)
 * can opt into this contract for diagnostics and interoperability logic.
 */
@KonditionalInternalApi
data class EvaluationDiagnostics<T : Any>(
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

        data class Rule(
            val matched: RuleMatch,
            val skippedByRollout: RuleMatch? = null,
        ) : Decision

        data class Default(
            val skippedByRollout: RuleMatch? = null,
        ) : Decision
    }

    data class RuleMatch(
        val rule: RuleExplanation,
        val bucket: BucketInfo,
    )

    enum class ExtensionType {
        NONE,
        LAMBDA,
    }

    enum class ConditionalContextType {
        NONE,
        NARROWING,
    }

    sealed interface TargetingNode {
        data class All(
            val children: List<TargetingNode>,
        ) : TargetingNode

        data class AnyOf(
            val children: List<TargetingNode>,
        ) : TargetingNode

        data class Locale(
            val ids: Set<String>,
        ) : TargetingNode

        data class Platform(
            val ids: Set<String>,
        ) : TargetingNode

        data class Version(
            val range: VersionRange,
        ) : TargetingNode

        data class Axis(
            val axisId: String,
            val allowedIds: Set<String>,
        ) : TargetingNode

        data object Custom : TargetingNode

        data class Guarded(
            val child: TargetingNode,
        ) : TargetingNode
    }

    data class ExtensionNode(
        val type: ExtensionType,
        val content: TargetingNode? = null,
    )

    data class ConditionalContextNode(
        val type: ConditionalContextType,
        val content: TargetingNode? = null,
    )

    data class RuleExplanation(
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
        val ruleId: String,
        val extensionNode: ExtensionNode = ExtensionNode(ExtensionType.NONE),
        val conditionalContextNode: ConditionalContextNode = ConditionalContextNode(ConditionalContextType.NONE),
    )
}
