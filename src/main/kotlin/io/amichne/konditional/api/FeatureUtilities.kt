package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.ContextAware
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.features.FeatureAware
import io.amichne.konditional.core.ops.EvaluationMetric
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.rules.Rule
import kotlin.system.measureNanoTime

inline fun <reified T : Any, reified C : Context, M : Namespace, D> D.feature(
    block: D.() -> Feature<T, C, M>,
): T where D : ContextAware<C>, D : FeatureAware<out M> = block().evaluate(context)

/**
 * Evaluate with an explicit context instance.
 */
inline fun <reified T : Any, reified C : Context, D> D.feature(
    context: C,
    @KonditionalDsl block: D.() -> Feature<T, C, *>,
): T where D : FeatureAware<*>, D : ContextAware<*> = block().evaluate(context)

/**
 * Evaluates this feature for the given contextFn.
 *
 * By default, evaluates using the feature's namespace registry.
 * For testing, you can provide an explicit registry parameter.
 *
 * @param context The evaluation contextFn
 * @param registry The registry to use (defaults to the feature's namespace)
 * @return The evaluated value, or null if the feature is not registered
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T {
    return evaluateInternal(context, registry, mode = EvaluationMetric.EvaluationMode.NORMAL).value
}

fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T> = evaluateInternal(context, registry, mode = EvaluationMetric.EvaluationMode.EXPLAIN)

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternal(
    context: C,
    registry: NamespaceRegistry,
    mode: EvaluationMetric.EvaluationMode,
): EvaluationResult<T> {
    val definition = registry.flag(this)
    lateinit var result: EvaluationResult<T>

    val nanos = measureNanoTime {
        result = when {
            registry.isAllDisabled -> EvaluationResult(
                namespaceId = registry.namespaceId,
                featureKey = key,
                configVersion = registry.configuration.metadata.version,
                mode = mode,
                durationNanos = 0L,
                value = definition.defaultValue,
                decision = EvaluationResult.Decision.RegistryDisabled,
            )
            !definition.isActive -> EvaluationResult(
                namespaceId = registry.namespaceId,
                featureKey = key,
                configVersion = registry.configuration.metadata.version,
                mode = mode,
                durationNanos = 0L,
                value = definition.defaultValue,
                decision = EvaluationResult.Decision.Inactive,
            )
            else -> {
                val trace = definition.evaluateTrace(context)

                val skipped = trace.skippedByRollout?.toRuleMatch(
                    bucket = trace.bucket,
                    featureKey = key,
                    salt = definition.salt,
                )

                val decision = trace.matched?.toRuleMatch(
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

    if (mode == EvaluationMetric.EvaluationMode.EXPLAIN) {
        registry.hooks.logger.debug {
            "konditional.explain namespaceId=${result.namespaceId} key=${result.featureKey} decision=${result.decision::class.simpleName} version=${result.configVersion}"
        }
    }

    registry.hooks.metrics.recordEvaluation(
        EvaluationMetric(
            namespaceId = registry.namespaceId,
            featureKey = key,
            mode = mode,
            durationNanos = nanos,
            decision = when (val decision = result.decision) {
                is EvaluationResult.Decision.RegistryDisabled -> EvaluationMetric.DecisionKind.REGISTRY_DISABLED
                is EvaluationResult.Decision.Inactive -> EvaluationMetric.DecisionKind.INACTIVE
                is EvaluationResult.Decision.Rule -> EvaluationMetric.DecisionKind.RULE
                is EvaluationResult.Decision.Default -> EvaluationMetric.DecisionKind.DEFAULT
            },
            configVersion = result.configVersion,
            bucket = when (val decision = result.decision) {
                is EvaluationResult.Decision.Rule -> decision.matched.bucket.bucket
                is EvaluationResult.Decision.Default -> decision.skippedByRollout?.bucket?.bucket
                else -> null
            },
            matchedRuleSpecificity = (result.decision as? EvaluationResult.Decision.Rule)?.matched?.rule?.totalSpecificity,
        )
    )

    return result
}

private fun <T : Any, C : Context> io.amichne.konditional.rules.ConditionalValue<T, C>.toRuleMatch(
    bucket: Int?,
    featureKey: String,
    salt: String,
): EvaluationResult.RuleMatch {
    requireNotNull(bucket) { "Bucket must be computed when a rule matches by criteria" }
    val explanation = rule.toExplanation()
    return EvaluationResult.RuleMatch(
        rule = explanation,
        bucket = BucketInfo(
            featureKey = featureKey,
            salt = salt,
            bucket = bucket,
            rollout = explanation.rollout,
            thresholdBasisPoints = io.amichne.konditional.core.evaluation.Bucketing.rolloutThresholdBasisPoints(explanation.rollout),
            inRollout = io.amichne.konditional.core.evaluation.Bucketing.isInRollout(explanation.rollout, bucket),
        ),
    )
}

private fun <C : Context> Rule<C>.toExplanation(): EvaluationResult.RuleExplanation {
    val base = baseEvaluable
    val extensionClassName = when (extension) {
        io.amichne.konditional.rules.evaluable.Placeholder -> null
        else -> extension::class.qualifiedName
    }

    val baseSpecificity =
        (if (base.locales.isNotEmpty()) 1 else 0) +
            (if (base.platforms.isNotEmpty()) 1 else 0) +
            (if (base.versionRange.hasBounds()) 1 else 0) +
            base.axisConstraints.size

    val extensionSpecificity = extension.specificity()

    return EvaluationResult.RuleExplanation(
        note = note,
        rollout = rollout,
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
