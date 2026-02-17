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
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.targeting.axesOrEmpty
import io.amichne.konditional.rules.targeting.customLeafCount
import io.amichne.konditional.rules.targeting.localesOrEmpty
import io.amichne.konditional.rules.targeting.platformsOrEmpty
import io.amichne.konditional.rules.targeting.versionRangeOrNull
import io.amichne.konditional.rules.versions.Unbounded
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
    lateinit var base: EvaluationDiagnostics<T>
    val nanos = measureNanoTime { base = createBaseDiagnostics(context, registry, mode, definition) }
    val result = base.copy(durationNanos = nanos)

    logExplainIfNeeded(result, registry, mode)
    recordEvaluationMetrics(result, registry, mode, nanos)

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

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.createBaseDiagnostics(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
    definition: FlagDefinition<T, C, M>,
): EvaluationDiagnostics<T> =
    when {
        registry.isAllDisabled ->
            EvaluationDiagnostics(
                namespaceId = registry.namespaceId,
                featureKey = key,
                configVersion = registry.configuration.metadata.version,
                mode = mode,
                durationNanos = 0L,
                value = definition.defaultValue,
                decision = EvaluationDiagnostics.Decision.RegistryDisabled,
            )

        !definition.isActive ->
            EvaluationDiagnostics(
                namespaceId = registry.namespaceId,
                featureKey = key,
                configVersion = registry.configuration.metadata.version,
                mode = mode,
                durationNanos = 0L,
                value = definition.defaultValue,
                decision = EvaluationDiagnostics.Decision.Inactive,
            )

        else -> createRuleDiagnostics(context, registry, mode, definition)
    }

private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.createRuleDiagnostics(
    context: C,
    registry: NamespaceRegistry,
    mode: Metrics.Evaluation.EvaluationMode,
    definition: FlagDefinition<T, C, M>,
): EvaluationDiagnostics<T> {
    val trace = definition.evaluateTrace(context)
    val skippedByRollout =
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
                    skippedByRollout = skippedByRollout,
                )
            }
            ?: EvaluationDiagnostics.Decision.Default(skippedByRollout = skippedByRollout)

    return EvaluationDiagnostics(
        namespaceId = registry.namespaceId,
        featureKey = key,
        configVersion = registry.configuration.metadata.version,
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
    val targeting = targeting
    val locales = targeting.localesOrEmpty()
    val platforms = targeting.platformsOrEmpty()
    val versionRange = targeting.versionRangeOrNull() ?: Unbounded
    val axes = targeting.axesOrEmpty()

    val customLeaves = targeting.customLeafCount()

    val baseSpecificity =
        (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0) +
            axes.size

    val extensionSpecificity = customLeaves

    return EvaluationDiagnostics.RuleExplanation(
        note = note,
        rollout = rampUp,
        locales = locales,
        platforms = platforms,
        versionRange = versionRange,
        axes = axes,
        baseSpecificity = baseSpecificity,
        extensionSpecificity = extensionSpecificity,
        totalSpecificity = targeting.specificity(),
        extensionClassName = null,
    )
}
