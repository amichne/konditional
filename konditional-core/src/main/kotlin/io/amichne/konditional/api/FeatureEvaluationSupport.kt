package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.EvaluationSnapshot
import io.amichne.konditional.core.registry.NamespaceRegistry

internal fun logExplainIfNeeded(
    snapshot: EvaluationSnapshot,
    result: EvaluationResult<*>,
    mode: Metrics.Evaluation.EvaluationMode,
) {
    if (mode == Metrics.Evaluation.EvaluationMode.EXPLAIN) {
        snapshot.hooks.logger.debug {
            "konditional.explain " +
                "namespaceId=${result.namespaceId} " +
                "key=${result.featureKey} " +
                "decision=${result.decision::class.simpleName} " +
                "version=${result.configVersion}"
        }
    }
}

internal fun recordEvaluation(
    snapshot: EvaluationSnapshot,
    result: EvaluationResult<*>,
    nanos: Long,
) {
    snapshot.hooks.metrics.recordEvaluation(
        Metrics.Evaluation(
            namespaceId = snapshot.namespaceId,
            featureKey = result.featureKey,
            mode = result.mode,
            durationNanos = nanos,
            decision =
                when (result.decision) {
                    is EvaluationResult.Decision.RegistryDisabled -> Metrics.Evaluation.DecisionKind.REGISTRY_DISABLED
                    is EvaluationResult.Decision.Inactive -> Metrics.Evaluation.DecisionKind.INACTIVE
                    is EvaluationResult.Decision.Rule -> Metrics.Evaluation.DecisionKind.RULE
                    is EvaluationResult.Decision.Default -> Metrics.Evaluation.DecisionKind.DEFAULT
                },
            configVersion = result.configVersion,
            bucket =
                when (result.decision) {
                    is EvaluationResult.Decision.Rule -> result.decision.matched.bucket.bucket
                    is EvaluationResult.Decision.Default -> result.decision.skippedByRollout?.bucket?.bucket
                    else -> null
                },
            matchedRuleSpecificity =
                (result.decision as? EvaluationResult.Decision.Rule)
                    ?.matched
                    ?.rule
                    ?.totalSpecificity,
        ),
    )
}

/**
 * Internal evaluation entrypoint used by sibling modules (e.g. shadow evaluation).
 *
 * Prefer [evaluate] / [explain] for application usage.
 */
@KonditionalInternalApi
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternalApi(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> = evaluateInternal(context, registry, mode)
