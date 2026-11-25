package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.evaluation.Bucketing.isInRampUp
import io.amichne.konditional.core.evaluation.Bucketing.rampUpThresholdBasisPoints
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.EvaluationSnapshot
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.targeting.axesOrEmpty
import io.amichne.konditional.rules.targeting.customLeafCount
import io.amichne.konditional.rules.targeting.localesOrEmpty
import io.amichne.konditional.rules.targeting.platformsOrEmpty
import io.amichne.konditional.rules.targeting.versionRangeOrNull
import io.amichne.konditional.rules.versions.Unbounded
import java.security.MessageDigest
import kotlin.time.measureTimedValue

private const val MINIMUM_ORDINAL = 0
private const val RULE_ID_HASH_LENGTH = 8

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
        trace.skippedByRampUp?.toRuleMatch(
            bucket = trace.bucket,
            featureKey = key,
            namespaceId = snapshot.namespaceId,
            salt = definition.salt,
            ruleOrdinal = definition.valuesByPrecedence.indexOfFirst { it === trace.skippedByRampUp },
        )
    val decision =
        trace.matched
            ?.toRuleMatch(
                bucket = trace.bucket,
                featureKey = key,
                namespaceId = snapshot.namespaceId,
                salt = definition.salt,
                ruleOrdinal = definition.valuesByPrecedence.indexOfFirst { it === trace.matched },
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
    namespaceId: String,
    salt: String,
    ruleOrdinal: Int,
): EvaluationDiagnostics.RuleMatch<EvaluationDiagnostics.RuleExplanation> = bucket?.let { bucket ->
    rule.toExplanation(ruleId = createRuleId(namespaceId, featureKey, ruleOrdinal)).let {
        EvaluationDiagnostics.RuleMatch(
            rule = it,
            bucket = BucketInfo(
                featureKey = featureKey,
                salt = salt,
                bucket = bucket,
                rollout = it.rollout,
                thresholdBasisPoints = rampUpThresholdBasisPoints(it.rollout),
                inRollout = isInRampUp(it.rollout, bucket),
            ),
        )
    }
} ?: error("Bucket must be computed when a rule matches by criteria")

private fun <C : Context> Rule<C>.toExplanation(
    ruleId: String,
): EvaluationDiagnostics.RuleExplanation {
    val tree = targeting.toTargetingNode()
    val locales = targeting.localesOrEmpty()
    val platforms = targeting.platformsOrEmpty()
    val versionRange = targeting.versionRangeOrNull() ?: Unbounded
    val axes = targeting.axesOrEmpty().mapValues { (_, values) -> values.toSet() }
    return EvaluationDiagnostics.RuleExplanation(
        note = note,
        rollout = rampUp,
        locales = locales,
        platforms = platforms,
        versionRange = versionRange,
        axes = axes,
        baseSpecificity = (if (locales.isNotEmpty()) 1 else 0) +
            (if (platforms.isNotEmpty()) 1 else 0) +
            (if (versionRange.hasBounds()) 1 else 0) + axes.size,
        extensionSpecificity = targeting.customLeafCount(),
        totalSpecificity = targeting.specificity(),
        extensionClassName = null,
        ruleId = ruleId,
        extensionNode = targeting.toExtensionNode(tree),
        conditionalContextNode = targeting.toConditionalContextNode(tree),
        targeting = tree,
    )
}

private fun createRuleId(namespaceId: String, featureKey: String, ruleOrdinal: Int): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val input = "$namespaceId:$featureKey:${ruleOrdinal.coerceAtLeast(MINIMUM_ORDINAL)}"
    val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
    val shortHash = hash.take(RULE_ID_HASH_LENGTH).joinToString("") { "%02x".format(it) }
    return "rule::$namespaceId::$featureKey::$shortHash"
}

private fun <C : Context> Targeting.All<C>.toExtensionNode(
    tree: EvaluationDiagnostics.TargetingNode,
): EvaluationDiagnostics.ExtensionNode {
    val hasExtension =
        targets.any { it is Targeting.Custom<*> || (it is Targeting.Guarded<*, *> && it.inner is Targeting.Custom<*>) }
    return if (!hasExtension) {
        EvaluationDiagnostics.ExtensionNode(EvaluationDiagnostics.ExtensionType.NONE)
    } else {
        EvaluationDiagnostics.ExtensionNode(
            type = EvaluationDiagnostics.ExtensionType.LAMBDA,
            content = tree,
        )
    }
}

private fun <C : Context> Targeting.All<C>.toConditionalContextNode(
    tree: EvaluationDiagnostics.TargetingNode,
): EvaluationDiagnostics.ConditionalContextNode {
    val hasNarrowing = targets.any { it is Targeting.Guarded<*, *> }
    return if (!hasNarrowing) {
        EvaluationDiagnostics.ConditionalContextNode(EvaluationDiagnostics.ConditionalContextType.NONE)
    } else {
        EvaluationDiagnostics.ConditionalContextNode(
            type = EvaluationDiagnostics.ConditionalContextType.NARROWING,
            content = tree,
        )
    }
}

private fun Targeting<*>.toTargetingNode(): EvaluationDiagnostics.TargetingNode =
    when (this) {
        is Targeting.All<*> -> EvaluationDiagnostics.TargetingNode.All(children = targets.map { it.toTargetingNode() })
        is Targeting.AnyOf<*> ->
            EvaluationDiagnostics.TargetingNode.AnyOf(
                children = targets.map { it.toTargetingNode() },
            )
        is Targeting.Locale -> EvaluationDiagnostics.TargetingNode.Locale(ids = ids.toSet())
        is Targeting.Platform -> EvaluationDiagnostics.TargetingNode.Platform(ids = ids.toSet())
        is Targeting.Version -> EvaluationDiagnostics.TargetingNode.Version(range = range)
        is Targeting.Axis -> EvaluationDiagnostics.TargetingNode.Axis(axisId = axisId, allowedIds = allowedIds.toSet())
        is Targeting.Attribute<*> -> EvaluationDiagnostics.TargetingNode.Attribute(key, operands)
        is Targeting.Predicate<*> -> EvaluationDiagnostics.TargetingNode.Predicate(key, version, argument)
        is Targeting.Custom<*> -> EvaluationDiagnostics.TargetingNode.Custom
        is Targeting.Guarded<*, *> ->
            EvaluationDiagnostics.TargetingNode.Guarded(
                child = inner.toTargetingNode(),
            )
    }
