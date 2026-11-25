package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.Bucketing.isInRollout
import io.amichne.konditional.core.evaluation.Bucketing.rolloutThresholdBasisPoints
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.EvaluationSnapshot
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import kotlin.time.measureTimedValue

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
): T = evaluateInternal(context, EvaluationSnapshot(registry), mode = Metrics.Evaluation.EvaluationMode.NORMAL).value

/**
 * Explains how this feature was evaluated for the given context.
 *
 * This returns the diagnostics snapshot used by Konditional internals:
 * matched/default decision, optional rollout skip details, and computed bucket metadata.
 *
 * @param context The evaluation context
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return Deterministic evaluation diagnostics for this input and registry snapshot
 * @throws IllegalStateException if the feature is not registered in the registry
 */

fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationDiagnostics<T> = evaluateInternal(context, EvaluationSnapshot(registry), mode = Metrics.Evaluation.EvaluationMode.EXPLAIN)

internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    snapshot: EvaluationSnapshot,
    mode: Metrics.Evaluation.EvaluationMode,
): EvaluationDiagnostics<T> {
    val definition = snapshot.flag(this)
    return measureTimedValue { createBaseDiagnostics(context, snapshot, mode, definition) }.also {
        logExplainIfNeeded(it.value, snapshot.registry, mode)
        recordEvaluationMetrics(it.value, snapshot.registry, mode, it.duration)
    }.value
}

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.createBaseDiagnostics(
    context: C,
    snapshot: EvaluationSnapshot,
    mode: Metrics.Evaluation.EvaluationMode,
    definition: FlagDefinition<T, C, M>,
): EvaluationDiagnostics<T> =
    when {
        snapshot.allDisabled ->
            EvaluationDiagnostics(
                namespaceId = snapshot.namespaceId,
                featureKey = key,
                configVersion = snapshot.version,
                mode = mode,
                durationNanos = 0L,
                value = declaredDefaultValueOr(definition.defaultValue),
                decision = EvaluationDiagnostics.Decision.RegistryDisabled,
            )

        !definition.isActive ->
            EvaluationDiagnostics(
                namespaceId = snapshot.namespaceId,
                featureKey = key,
                configVersion = snapshot.version,
                mode = mode,
                durationNanos = 0L,
                value = definition.defaultValue,
                decision = EvaluationDiagnostics.Decision.Inactive,
            )

        else -> createRuleDiagnostics(context, snapshot, mode, definition)
    }

@Suppress("UNCHECKED_CAST")
private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.declaredDefaultValueOr(
    fallback: T,
): T =
    (namespace.declaredDefault(this) as? T) ?: fallback

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.createRuleDiagnostics(
    context: C,
    snapshot: EvaluationSnapshot,
    mode: Metrics.Evaluation.EvaluationMode,
    definition: FlagDefinition<T, C, M>,
): EvaluationDiagnostics<T> {
    val trace = definition.evaluateTrace(
        context = context,
        registry = snapshot,
    )
    val skippedByRollout =
        trace.skippedByRollout?.toRuleMatch(
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
                    skippedByRollout = skippedByRollout,
                )
            }
            ?: EvaluationDiagnostics.Decision.Default(skippedByRollout = skippedByRollout)

    return EvaluationDiagnostics(
        namespaceId = snapshot.namespaceId,
        featureKey = key,
        configVersion = snapshot.version,
        mode = mode,
        durationNanos = 0L,
        value = trace.value,
        decision = decision,
    )
}

private fun <T : Any, C : Context> ConditionalValue<T, C>.toRuleMatch(
    bucket: Int?,
    featureKey: String,
    salt: String,
): EvaluationDiagnostics.RuleMatch<EvaluationDiagnostics.RuleExplanation> = bucket?.let { bucket ->
    rule.toExplanation().let {
        EvaluationDiagnostics.RuleMatch(
            rule = it,
            bucket = BucketInfo(
                featureKey = featureKey,
                salt = salt,
                bucket = bucket,
                rollout = it.rollout,
                thresholdBasisPoints = rolloutThresholdBasisPoints(it.rollout),
                inRollout = isInRollout(it.rollout, bucket),
            ),
        )
    }
} ?: error("Bucket must be computed when a rule matches by criteria")

private fun Rule<*>.toExplanation(): EvaluationDiagnostics.RuleExplanation = EvaluationDiagnostics.RuleExplanation(
    note = note,
    rollout = rollout,
    targeting = targeting.toTargetingNode(),
    totalSpecificity = specificity(),
    ruleId = ruleId,
)

private fun Targeting<*>.toTargetingNode(): EvaluationDiagnostics.TargetingNode = when (this) {
    is Targeting.All<*> -> EvaluationDiagnostics.TargetingNode.All(targets.map { it.toTargetingNode() })
    is Targeting.AnyOf<*> -> EvaluationDiagnostics.TargetingNode.AnyOf(targets.map { it.toTargetingNode() })
    is Targeting.Facts -> EvaluationDiagnostics.TargetingNode.Facts(expression.spec)
    is Targeting.Custom<*> -> EvaluationDiagnostics.TargetingNode.Custom
    is Targeting.Guarded<*, *> -> EvaluationDiagnostics.TargetingNode.Guarded(inner.toTargetingNode())
}
