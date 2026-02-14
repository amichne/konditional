@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.Bucketing.isInRampUp
import io.amichne.konditional.core.evaluation.Bucketing.rampUpThresholdBasisPoints
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.Rule
import kotlin.system.measureNanoTime

/**
 * Evaluates this feature for the given context.
 *
 * By default, evaluates using the feature's namespace registry.
 * For testing, you can provide an explicit registry parameter.
 *
 * Example:
 * ```kotlin
 * val enabled = AppFlags.darkMode.evaluate(context)
 * ```
 *
 * @param context The evaluation context
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return The evaluated value
 * @throws IllegalStateException if the feature is not registered in the registry
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T = evaluateInternal(context, registry, mode = Metrics.Evaluation.EvaluationMode.NORMAL).value

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationDiagnostics<T> = evaluateInternal(
    context = context,
    registry = registry,
    mode = mode,
    definition = registry.flag(this),
)

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
    definition: FlagDefinition<T, C, M>,
): EvaluationDiagnostics<T> {
    lateinit var result: EvaluationDiagnostics<T>

    val nanos =
        measureNanoTime {
            result =
                when {
                    registry.isAllDisabled -> {
                        EvaluationDiagnostics(
                            namespaceId = registry.namespaceId,
                            featureKey = key,
                            configVersion = registry.configuration.metadata.version,
                            mode = mode,
                            durationNanos = 0L,
                            value = definition.defaultValue,
                            decision = EvaluationDiagnostics.Decision.RegistryDisabled,
                        )
                    }

                    !definition.isActive -> {
                        EvaluationDiagnostics(
                            namespaceId = registry.namespaceId,
                            featureKey = key,
                            configVersion = registry.configuration.metadata.version,
                            mode = mode,
                            durationNanos = 0L,
                            value = definition.defaultValue,
                            decision = EvaluationDiagnostics.Decision.Inactive,
                        )
                    }

                    else -> {
                        val trace = definition.evaluateTrace(context)

                        val skipped =
                            trace.skippedByRampUp?.toRuleMatch(
                                bucket = trace.bucket,
                                featureKey = key,
                                salt = definition.salt,
                            )

                        val decision =
                            trace.matched
                                ?.toRuleMatch(
                                    bucket = trace.bucket,
                                    featureKey = key,
                                    salt = definition.salt,
                                )?.let { matched ->
                                    EvaluationDiagnostics.Decision.Rule(
                                        matched = matched,
                                        skippedByRollout = skipped,
                                    )
                                } ?: EvaluationDiagnostics.Decision.Default(skippedByRollout = skipped)

                        EvaluationDiagnostics(
                            namespaceId = registry.namespaceId,
                            featureKey = key,
                            configVersion = registry.configuration.metadata.version,
                            mode = mode,
                            durationNanos = 0L,
                            value = trace.value,
                            decision = decision,
                        )
                    }
                }
        }

    result = result.copy(durationNanos = nanos)

    if (mode == Metrics.Evaluation.EvaluationMode.EXPLAIN) {
        registry.hooks.logger.debug {
            "konditional.explain namespaceId=${result.namespaceId} key=${result.featureKey} decision=${result.decision::class.simpleName} version=${result.configVersion}"
        }
    }

    registry.hooks.metrics.recordEvaluation(
        Metrics.Evaluation(
            namespaceId = registry.namespaceId,
            featureKey = key,
            mode = mode,
            durationNanos = nanos,
            decision =
                when (result.decision) {
                    is EvaluationDiagnostics.Decision.RegistryDisabled -> Metrics.Evaluation.DecisionKind.REGISTRY_DISABLED
                    is EvaluationDiagnostics.Decision.Inactive -> Metrics.Evaluation.DecisionKind.INACTIVE
                    is EvaluationDiagnostics.Decision.Rule -> Metrics.Evaluation.DecisionKind.RULE
                    is EvaluationDiagnostics.Decision.Default -> Metrics.Evaluation.DecisionKind.DEFAULT
                },
            configVersion = result.configVersion,
            bucket =
                when (result.decision) {
                    is EvaluationDiagnostics.Decision.Rule -> {
                        result.decision.matched.bucket.bucket
                    }

                    is EvaluationDiagnostics.Decision.Default -> {
                        result.decision.skippedByRollout
                            ?.bucket
                            ?.bucket
                    }

                    else -> {
                        null
                    }
                },
            matchedRuleSpecificity = (result.decision as? EvaluationDiagnostics.Decision.Rule)?.matched?.rule?.totalSpecificity,
        ),
    )

    return result
}

/**
 * Internal evaluation entrypoint used by sibling modules (e.g. shadow evaluation).
 *
 * Prefer [evaluate] for application usage.
 */
@KonditionalInternalApi
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternalApi(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationDiagnostics<T> = evaluateInternal(context, registry, mode)

private fun <T : Any, C : Context> ConditionalValue<T, C>.toRuleMatch(
    bucket: Int?,
    featureKey: String,
    salt: String,
): EvaluationDiagnostics.RuleMatch {
    requireNotNull(bucket) { "Bucket must be computed when a rule matches by criteria" }
    val explanation = rule.toExplanation()
    return EvaluationDiagnostics.RuleMatch(
        rule = explanation,
        bucket =
            BucketInfo(
                featureKey = featureKey,
                salt = salt,
                bucket = bucket,
                rollout = explanation.rollout,
                thresholdBasisPoints =
                    rampUpThresholdBasisPoints(
                        explanation.rollout,
                    ),
                inRollout = isInRampUp(explanation.rollout, bucket),
            ),
    )
}

private fun <C : Context> Rule<C>.toExplanation(): EvaluationDiagnostics.RuleExplanation {
    val base = targeting
    val extensionClassName =
        when (predicate) {
            io.amichne.konditional.rules.evaluable.Placeholder -> null
            else -> predicate::class.qualifiedName
        }

    val baseSpecificity =
        (if (base.locales.isNotEmpty()) 1 else 0) +
            (if (base.platforms.isNotEmpty()) 1 else 0) +
            (if (base.versionRange.hasBounds()) 1 else 0) +
            base.axisConstraints.size

    val extensionSpecificity = predicate.specificity()

    return EvaluationDiagnostics.RuleExplanation(
        note = note,
        rollout = rampUp,
        locales = base.locales,
        platforms = base.platforms,
        versionRange = base.versionRange,
        axes = base.axisConstraints.associate { it.axisId to it.allowedIds },
        baseSpecificity = baseSpecificity,
        extensionSpecificity = extensionSpecificity,
        totalSpecificity = baseSpecificity + extensionSpecificity,
        extensionClassName = extensionClassName,
    )
}
