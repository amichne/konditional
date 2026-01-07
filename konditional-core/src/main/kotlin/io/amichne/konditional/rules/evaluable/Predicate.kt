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
 * - Multiple Predicate instances can be combined (e.g., [io.amichne.konditional.rules.Rule] composes [BasePredicate] with extension logic)
 * - Specificity values can be summed to determine rule precedence
 * - Matching logic can be chained (all must match for composition to match)
 *
 * @param C The contextFn type that this evaluable evaluates against
 *
 * @see io.amichne.konditional.rules.Rule
 * @see BasePredicate
 */
fun interface Predicate<in C : Context> : Specifier {
    /**
     * Determines if this evaluable matches the given contextFn.
     *
     * The default implementation always returns true, allowing implementations
     * to selectively override only when they need custom matching logic.
     *
     * @param context The contextFn to evaluate against
     * @return true if the contextFn matches this evaluable's criteria, false otherwise
     */
    fun matches(context: C): Boolean

    companion object {
        fun <C : Context> factory(matcher: (C) -> Boolean): Predicate<C> = Predicate { context -> matcher(context) }
    }
}
