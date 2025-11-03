package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

/**
 * Base abstraction for composable rule evaluation logic.
 *
 * This class provides the foundation for building composable rule evaluation systems.
 * Implementations can define custom matching criteria and specificity calculations that
 * can be composed together to create complex rule systems.
 *
 * The design enables composition through:
 * - Multiple Evaluable instances can be combined (e.g., [io.amichne.konditional.rules.Rule] composes [UserClientEvaluator] with extension logic)
 * - Specificity values can be summed to determine rule precedence
 * - Matching logic can be chained (all must match for composition to match)
 *
 * @param C The context type that this evaluable evaluates against
 *
 * @see io.amichne.konditional.rules.Rule
 * @see UserClientEvaluator
 */
abstract class Evaluable<C : Context> {
    /**
     * Determines if this evaluable matches the given context.
     *
     * The default implementation always returns true, allowing implementations
     * to selectively override only when they need custom matching logic.
     *
     * @param context The context to evaluate against
     * @return true if the context matches this evaluable's criteria, false otherwise
     */
    open fun matches(context: C): Boolean = true

    /**
     * Calculates the specificity of this evaluable.
     *
     * Specificity determines precedence when multiple rules could match - higher values
     * are evaluated first. The default implementation returns 0, representing no specificity.
     *
     * When composing multiple Evaluables, their specificity values should be summed to
     * calculate the total specificity of the composition.
     *
     * @return The specificity value (higher is more specific)
     */
    open fun specificity(): Int = 0
}
