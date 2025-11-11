package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

/**
 * Factory object for creating common [Evaluable] instances.
 *
 * This object provides convenient factory methods for building evaluables
 * without needing to create custom subclasses for common use cases.
 *
 * Example usage:
 * ```kotlin
 * rule {
 *     extension { Evaluables.predicate { context ->
 *         context.userId in allowedUsers
 *     }}
 * }
 * ```
 *
 * @since 0.0.2
 */
object Evaluables {
    /**
     * Creates an evaluable from a simple predicate function.
     *
     * This is the most flexible factory method, allowing you to specify
     * custom matching logic with optional specificity and description.
     *
     * Example:
     * ```kotlin
     * Evaluables.predicate(
     *     specificity = 5,
     *     description = "Beta users only"
     * ) { context ->
     *     context.userTier == UserTier.BETA
     * }
     * ```
     *
     * @param specificity The specificity value for rule precedence (default: 1)
     * @param description Optional human-readable description of this evaluable
     * @param predicate The matching function that evaluates the context
     * @return An Evaluable that matches when the predicate returns true
     */
    fun <C : Context> predicate(
        specificity: Int = 1,
        description: String? = null,
        predicate: (C) -> Boolean
    ): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = predicate(context)
        override fun specificity(): Int = specificity
        override fun toString(): String = description ?: "Evaluable.predicate"
    }

    /**
     * Creates an evaluable that matches when ALL of the given evaluables match.
     *
     * The resulting evaluable's specificity is the sum of all component specificities.
     *
     * Example:
     * ```kotlin
     * Evaluables.allOf(
     *     Evaluables.predicate { it.isPremium },
     *     Evaluables.predicate { it.hasFeatureAccess("advanced") }
     * )
     * ```
     *
     * @param evaluables The evaluables that must all match
     * @return An Evaluable that matches when all components match
     */
    fun <C : Context> allOf(vararg evaluables: Evaluable<C>): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = evaluables.all { it.matches(context) }
        override fun specificity(): Int = evaluables.sumOf { it.specificity() }
        override fun toString(): String = "Evaluables.allOf(${evaluables.size} evaluables)"
    }

    /**
     * Creates an evaluable that matches when ANY of the given evaluables match.
     *
     * The resulting evaluable's specificity is the maximum of all component specificities.
     *
     * Example:
     * ```kotlin
     * Evaluables.anyOf(
     *     Evaluables.predicate { it.userTier == UserTier.ENTERPRISE },
     *     Evaluables.predicate { it.userTier == UserTier.BETA }
     * )
     * ```
     *
     * @param evaluables The evaluables where at least one must match
     * @return An Evaluable that matches when any component matches
     */
    fun <C : Context> anyOf(vararg evaluables: Evaluable<C>): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = evaluables.any { it.matches(context) }
        override fun specificity(): Int = evaluables.maxOfOrNull { it.specificity() } ?: 0
        override fun toString(): String = "Evaluables.anyOf(${evaluables.size} evaluables)"
    }

    /**
     * Creates an evaluable that inverts the matching logic of another evaluable.
     *
     * The resulting evaluable preserves the original's specificity.
     *
     * Example:
     * ```kotlin
     * Evaluables.not(
     *     Evaluables.predicate { it.isTestUser }
     * )
     * ```
     *
     * @param evaluable The evaluable to negate
     * @return An Evaluable that matches when the original does not match
     */
    fun <C : Context> not(evaluable: Evaluable<C>): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = !evaluable.matches(context)
        override fun specificity(): Int = evaluable.specificity()
        override fun toString(): String = "Evaluables.not($evaluable)"
    }

    /**
     * Creates an evaluable that always matches.
     *
     * Useful for rules that should apply to all users, potentially
     * with additional targeting based on rollout percentage.
     *
     * Example:
     * ```kotlin
     * rule {
     *     extension { Evaluables.always() }
     *     rollout = Rollout.of(10.0)
     * }.implies(true)
     * ```
     *
     * @return An Evaluable that always matches
     */
    fun <C : Context> always(): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = true
        override fun specificity(): Int = 0
        override fun toString(): String = "Evaluables.always()"
    }

    /**
     * Creates an evaluable that never matches.
     *
     * Useful as a placeholder or for temporarily disabling a rule
     * without removing it from the configuration.
     *
     * Example:
     * ```kotlin
     * rule {
     *     extension { Evaluables.never() }
     * }.implies(true)  // This rule will never trigger
     * ```
     *
     * @return An Evaluable that never matches
     */
    fun <C : Context> never(): Evaluable<C> = object : Evaluable<C>() {
        override fun matches(context: C): Boolean = false
        override fun specificity(): Int = 0
        override fun toString(): String = "Evaluables.never()"
    }
}
