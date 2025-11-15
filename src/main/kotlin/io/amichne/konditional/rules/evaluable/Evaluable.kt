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
 * - Multiple Evaluable instances can be combined (e.g., [io.amichne.konditional.rules.Rule] composes [BaseEvaluable] with extension logic)
 * - Specificity values can be summed to determine rule precedence
 * - Matching logic can be chained (all must match for composition to match)
 *
 * @param C The context type that this evaluable evaluates against
 *
 * @see io.amichne.konditional.rules.Rule
 * @see BaseEvaluable
 */
fun interface Evaluable<in C : Context> : Specifier {
    /**
     * Determines if this evaluable matches the given context.
     *
     * The default implementation always returns true, allowing implementations
     * to selectively override only when they need custom matching logic.
     *
     * @param context The context to evaluate against
     * @return true if the context matches this evaluable's criteria, false otherwise
     */
    fun matches(context: C): Boolean

    companion object {
        fun <C : Context> factory(matcher: (C) -> Boolean): Evaluable<C> = Evaluable { context -> matcher(context) }
    }
}
