package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.targeting.Targeting

/**
 * Adapter interface for custom evaluation logic.
 *
 * Prefer implementing [Targeting.Custom] directly for structural composability.
 * This interface exists as a compatibility bridge for named predicate classes
 * that predate the [Targeting] hierarchy.
 *
 * @param C The context type this predicate evaluates against.
 */
@Deprecated(
    message = "Implement Targeting.Custom<C> directly for structural composability.",
    replaceWith = ReplaceWith("Targeting.Custom", "io.amichne.konditional.rules.targeting.Targeting"),
)
fun interface Predicate<in C : Context> {

    /**
     * Determines if this predicate matches the given context.
     *
     * @param context The context to evaluate against.
     * @return true if the context matches this predicate's criteria, false otherwise.
     */
    fun matches(context: C): Boolean

    /**
     * Specificity contribution for precedence ordering.
     *
     * @return The specificity value (higher is more specific). Default is 1.
     */
    fun specificity(): Int = 1

    /** Converts this predicate to a [Targeting.Custom] leaf. */
    fun asTargeting(): Targeting.Custom<@UnsafeVariance C> =
        Targeting.Custom(block = ::matches, weight = specificity())

    companion object {
        fun <C : Context> factory(matcher: (C) -> Boolean): Predicate<C> = Predicate { context -> matcher(context) }
    }
}
