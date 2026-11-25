@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.api.evaluateInternal
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.registry.EvaluationSnapshot

/**
 * Typed receiver used by deferred rule values.
 *
 * Instances are created only during evaluation for a rule that already matched
 * its condition and rollout constraints.
 *
 * Determinism note:
 * keep resolver logic pure and deterministic for a fixed context + registry snapshot.
 */
class RuleValueScope<C : Context> @PublishedApi internal constructor(
    val context: C,
    private val evaluationRegistry: EvaluationSnapshot,
    private val ownerNamespace: Namespace<*>,
) {
    /**
     * Evaluates [Feature] using the current rule evaluation context.
     *
     * Same-namespace features are resolved against the current evaluation registry
     * to preserve shadow/baseline semantics. Cross-namespace features use their own
     * namespace registry.
     */
    fun <T : Any, M : Namespace<*>> Feature<T, C, M>.evaluate(): T =
        if (namespace === ownerNamespace) {
            evaluateInternal(context, evaluationRegistry, Metrics.Evaluation.EvaluationMode.NORMAL).value
        } else {
            evaluate(context = context)
        }

    /**
     * Evaluates [Feature] using invoke-style syntax in a deferred resolver.
     *
     * Equivalent to [evaluate], including same-namespace registry preservation.
     */
    operator fun <T : Any, M : Namespace<*>> Feature<T, C, M>.invoke(): T = evaluate()
}

/** Computes a rule's value from the evaluation context once the rule has matched. */
typealias RuleValueResolver<C, T> = RuleValueScope<C>.() -> T
