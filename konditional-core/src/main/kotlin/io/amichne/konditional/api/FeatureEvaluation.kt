@file:kotlin.jvm.JvmName("FeatureEvaluationKt")
@file:kotlin.jvm.JvmMultifileClass
@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.Bucketing.isInRampUp
import io.amichne.konditional.core.evaluation.Bucketing.rampUpThresholdBasisPoints
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.EvaluationSnapshot
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

/**
 * Evaluates this feature using a deterministic snapshot.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    snapshot: EvaluationSnapshot,
): T = evaluateInternal(context, snapshot, mode = Metrics.Evaluation.EvaluationMode.NORMAL).value

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
 * Explains evaluation using a deterministic snapshot.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
    context: C,
    snapshot: EvaluationSnapshot,
): EvaluationResult<T> = evaluateInternal(context, snapshot, mode = Metrics.Evaluation.EvaluationMode.EXPLAIN)

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> {
    val snapshot = registry.snapshot()
    val definition = registry.flag(this)

    return evaluateInternal(
        context = context,
        snapshot = snapshot,
        definition = definition,
        mode = mode,
    )
}

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    snapshot: EvaluationSnapshot,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> =
    evaluateInternal(
        context = context,
        snapshot = snapshot,
        definition = snapshot.flag(this),
        mode = mode,
    )

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    snapshot: EvaluationSnapshot,
    definition: io.amichne.konditional.core.FlagDefinition<T, C, M>,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> {
    lateinit var result: EvaluationResult<T>

    val nanos = measureNanoTime {
        result =
            buildEvaluationResult(
                context = context,
                snapshot = snapshot,
                definition = definition,
                mode = mode,
            )
    }

    result = result.copy(durationNanos = nanos)

    logExplainIfNeeded(snapshot, result, mode)
    recordEvaluation(snapshot, result, nanos)

    return result
}

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.buildEvaluationResult(
    context: C,
    snapshot: EvaluationSnapshot,
    definition: io.amichne.konditional.core.FlagDefinition<T, C, M>,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationResult<T> =
    when {
        snapshot.isAllDisabled -> EvaluationResult(
            namespaceId = snapshot.namespaceId,
            featureKey = key,
            configVersion = snapshot.configuration.metadata.version,
            mode = mode,
            durationNanos = 0L,
            value = definition.defaultValue,
            decision = EvaluationResult.Decision.RegistryDisabled,
        )
        !definition.isActive -> EvaluationResult(
            namespaceId = snapshot.namespaceId,
            featureKey = key,
            configVersion = snapshot.configuration.metadata.version,
            mode = mode,
            durationNanos = 0L,
            value = definition.defaultValue,
            decision = EvaluationResult.Decision.Inactive,
        )
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
                namespaceId = snapshot.namespaceId,
                featureKey = key,
                configVersion = snapshot.configuration.metadata.version,
                mode = mode,
                durationNanos = 0L,
                value = trace.value,
                decision = decision,
            )
        }
    }

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
