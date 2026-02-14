@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.Bucketing.isInRampUp
import io.amichne.konditional.core.evaluation.Bucketing.rampUpThresholdBasisPoints
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
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

/**
 * Evaluates this feature without throwing when the feature definition is absent.
 *
 * This API is useful at dynamic integration boundaries where callers prefer a typed failure
 * (`ParseError.FeatureNotFound`) over exception handling.
 *
 * @return [ParseResult.Success] with the evaluated value, or [ParseResult.Failure] when the feature is missing.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateSafely(
    context: C,
    registry: NamespaceRegistry = namespace,
): ParseResult<T> =
    registry.findFlag(this)?.let { definition ->
        ParseResult.success(
            evaluateInternal(
                context = context,
                registry = registry,
                mode = Metrics.Evaluation.EvaluationMode.NORMAL,
                definition = definition,
            ).value,
        )
    } ?: ParseResult.failure(ParseError.featureNotFound(id))

/**
 * Explains how this feature was evaluated for the given context.
 *
 * Returns detailed information about the evaluation decision, including which rule matched
 * (if any), the bucket assignment, and specificity values. Useful for debugging and
 * understanding feature flag behavior.
 *
 * Example:
 * ```kotlin
 * val result = feature.explain(context)
 * when (result.decision) {
 *     is Decision.Rule -> println("Matched rule: ${result.decision.matched.rule.note}")
 *     is Decision.Default -> println("No rule matched, using default")
 *     is Decision.Inactive -> println("Feature is inactive")
 *     is Decision.RegistryDisabled -> println("Registry is disabled")
 * }
 * ```
 *
 * @param context The evaluation contextFn
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return Detailed evaluation result with decision information
 * @throws IllegalStateException if the feature is not registered in the registry
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T> = evaluateInternal(context, registry, mode = Metrics.Evaluation.EvaluationMode.EXPLAIN)

/**
 * Explain variant that returns a typed failure when the feature definition is missing.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explainSafely(
    context: C,
    registry: NamespaceRegistry = namespace,
): ParseResult<EvaluationResult<T>> =
    registry.findFlag(this)?.let { definition ->
        ParseResult.success(
            evaluateInternal(
                context = context,
                registry = registry,
                mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
                definition = definition,
            ),
        )
    } ?: ParseResult.failure(ParseError.featureNotFound(id))

@Deprecated(
    message = "Use explain() instead for clearer intent",
    replaceWith = ReplaceWith("explain(context, registry)"),
    level = DeprecationLevel.WARNING
)
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T> = explain(context, registry)

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> = evaluateInternal(
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
): EvaluationResult<T> {
    lateinit var result: EvaluationResult<T>

    val nanos =
        measureNanoTime {
            result =
                when {
                    registry.isAllDisabled -> {
                        EvaluationResult(
                            namespaceId = registry.namespaceId,
                            featureKey = key,
                            configVersion = registry.configuration.metadata.version,
                            mode = mode,
                            durationNanos = 0L,
                            value = definition.defaultValue,
                            decision = EvaluationResult.Decision.RegistryDisabled,
                        )
                    }

                    !definition.isActive -> {
                        EvaluationResult(
                            namespaceId = registry.namespaceId,
                            featureKey = key,
                            configVersion = registry.configuration.metadata.version,
                            mode = mode,
                            durationNanos = 0L,
                            value = definition.defaultValue,
                            decision = EvaluationResult.Decision.Inactive,
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
                                    EvaluationResult.Decision.Rule(matched = matched, skippedByRollout = skipped)
                                } ?: EvaluationResult.Decision.Default(skippedByRollout = skipped)

                        EvaluationResult(
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
                    is EvaluationResult.Decision.RegistryDisabled -> Metrics.Evaluation.DecisionKind.REGISTRY_DISABLED
                    is EvaluationResult.Decision.Inactive -> Metrics.Evaluation.DecisionKind.INACTIVE
                    is EvaluationResult.Decision.Rule -> Metrics.Evaluation.DecisionKind.RULE
                    is EvaluationResult.Decision.Default -> Metrics.Evaluation.DecisionKind.DEFAULT
                },
            configVersion = result.configVersion,
            bucket =
                when (result.decision) {
                    is EvaluationResult.Decision.Rule -> {
                        result.decision.matched.bucket.bucket
                    }

                    is EvaluationResult.Decision.Default -> {
                        result.decision.skippedByRollout
                            ?.bucket
                            ?.bucket
                    }

                    else -> {
                        null
                    }
                },
            matchedRuleSpecificity = (result.decision as? EvaluationResult.Decision.Rule)?.matched?.rule?.totalSpecificity,
        ),
    )

    return result
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

private fun <T : Any, C : Context> ConditionalValue<T, C>.toRuleMatch(
    bucket: Int?,
    featureKey: String,
    salt: String,
): EvaluationResult.RuleMatch {
    requireNotNull(bucket) { "Bucket must be computed when a rule matches by criteria" }
    val explanation = rule.toExplanation()
    return EvaluationResult.RuleMatch(
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

private fun <C : Context> Rule<C>.toExplanation(): EvaluationResult.RuleExplanation {
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

    return EvaluationResult.RuleExplanation(
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
