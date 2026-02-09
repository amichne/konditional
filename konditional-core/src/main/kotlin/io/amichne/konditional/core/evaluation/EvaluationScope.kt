package io.amichne.konditional.core.evaluation

import io.amichne.konditional.api.EvaluationResult
import io.amichne.konditional.api.evaluateInternal
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.ConfigurationView
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.NamespaceRegistry

/**
 * Evaluation scope that guarantees referential transparency within a single evaluation chain.
 *
 * The scope captures the immutable configuration snapshot that triggered the evaluation and provides
 * lazy, cached access to other features in the same namespace.
 */
class EvaluationScope<out C : Context, M : Namespace> internal constructor(
    val context: C,
    internal val registry: NamespaceRegistry,
    internal val snapshot: ConfigurationView,
    internal val isAllDisabled: Boolean,
    internal val mode: Metrics.Evaluation.EvaluationMode,
) {
    private val cache = mutableMapOf<Feature<*, *, *>, EvaluationResult<*>>()

    fun <T : Any> value(feature: Feature<T, @UnsafeVariance C, M>): T = evaluate(feature).value

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> evaluate(feature: Feature<T, @UnsafeVariance C, M>): EvaluationResult<T> =
        cache[feature] as? EvaluationResult<T>
            ?: feature
                .evaluateInternal(
                    context = context,
                    registry = registry,
                    mode = mode,
                    scope = this,
                )
                .also { cache[feature] = it }

    operator fun <T : Any> Feature<T, @UnsafeVariance C, M>.invoke(): T = value(this)
}
