package io.amichne.konditional.rules.targeting

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactExpression
import io.amichne.konditional.context.fact.FactSchema

/**
 * Algebraic description of conditions under which a rule fires.
 *
 * Each leaf encodes exactly the context capability it requires via its type
 * parameter. A [Guarded] wrapper lifts any precisely-typed leaf into a common
 * `Targeting<C>` so heterogeneous constraints can be composed in [All].
 *
 * ## Invariants
 * - [matches] is pure and deterministic: same context produces the same result.
 * - [specificity] is structurally derived and stable across calls.
 * - [All] with an empty list matches everything (identity / catch-all).
 * - Non-matching semantics for capability absence: [Guarded.matches] returns
 *   `false`, never throws, when the context does not carry the required capability.
 *
 */
sealed interface Targeting<in C : Context> {

    /** Returns true iff [context] satisfies this targeting constraint. */
    fun matches(context: C): Boolean

    /**
     * Contribution to rule precedence ordering.
     *
     * Higher specificity means higher priority when multiple rules match.
     * [All] sums the specificity of all contained leaves.
     */
    fun specificity(): Int

    /** A portable predicate over facts declared by a namespace. */
    data class Facts(val expression: FactExpression) : Targeting<Context> {
        override fun matches(context: Context): Boolean = expression.matches(context)
        override fun specificity(): Int = expression.specificity()
    }

    /**
     * Custom predicate targeting.
     *
     * Evaluates [block] against the full context [C].
     * [weight] is the specificity contribution; default 1.
     * Prefer [Guarded] when the predicate only applies to a narrower context subtype.
     *
     * @param block Pure evaluation function. Must be deterministic for a given context.
     * @param weight Specificity contribution. Default 1.
     */
    data class Custom<C : Context>(
        val block: (C) -> Boolean,
        val weight: Int = 1,
    ) : Targeting<C> {
        override fun matches(context: C): Boolean = block(context)
        override fun specificity(): Int = weight
    }

    /**
     * Capability-narrowed predicate.
     *
     * Lifts a `Targeting<R>` (which requires context subtype [R]) into `Targeting<C>`.
     * When the runtime context does not implement [R], [matches] returns `false` without
     * throwing — non-matching semantics for absent capabilities.
     *
     * The [evidence] function captures the type narrowing; it is the **only** site in
     * the Targeting hierarchy where a runtime context cast is performed.
     *
     * @param inner The precisely-typed targeting leaf.
     * @param evidence Projects context [C] to [R], returning null if the capability is absent.
     */
    data class Guarded<C : Context, R : Context>(
        val inner: Targeting<R>,
        val evidence: (C) -> R?,
    ) : Targeting<C> {
        override fun matches(context: C): Boolean = evidence(context)?.let { inner.matches(it) } ?: false
        override fun specificity(): Int = inner.specificity()
    }

    /**
     * OR-disjunction of zero or more [Targeting] constraints.
     *
     * - Empty list never matches (annihilator element, dual of [All]'s identity).
     * - [specificity] is the structural maximum of all branch specificities; pure,
     *   no context required — consistent with the [specificity] contract.
     * - Groups that contain a Kotlin callback evaluate in process only.
     *
     * @param targets Ordered list of constraints; any one must match.
     */
    data class AnyOf<C : Context>(
        val targets: List<Targeting<C>>,
    ) : Targeting<C> {
        override fun matches(context: C): Boolean = targets.any { it.matches(context) }
        override fun specificity(): Int = targets.maxOfOrNull { it.specificity() } ?: 0
    }

    // -- Combinator ---------------------------------------------------------------

    /**
     * AND-conjunction of zero or more [Targeting] constraints.
     *
     * - Empty list matches everything (identity element / catch-all).
     * - [specificity] is the sum of all leaf specificities.
     * - [plus] produces a new [All] without mutating either operand.
     *
     * @param targets Ordered list of constraints; all must match.
     */
    data class All<C : Context>(
        val targets: List<Targeting<C>>,
    ) : Targeting<C> {
        override fun matches(context: C): Boolean = targets.all { it.matches(context) }
        override fun specificity(): Int = targets.sumOf { it.specificity() }
        operator fun plus(other: All<C>): All<C> = All(targets + other.targets)
    }

    companion object {
        /** Returns an [All] that matches every context (identity element). */
        fun <C : Context> catchAll(): All<C> = All(emptyList())

        /**
         * Lifts a capability-narrowed predicate into `Targeting<C>`.
         *
         * Intended for DSL use via `whenContext<R> { ... }`. The reified [R]
         * is captured in the [Guarded.evidence] lambda; no KClass is stored.
         *
         * @param weight Specificity contribution of the custom predicate.
         * @param block Evaluation function evaluated against the narrowed context [R].
         */
        inline fun <C : Context, reified R : C> whenContext(
            weight: Int = 1,
            crossinline block: R.() -> Boolean,
        ): Targeting<C> = Guarded<C, R>(
            inner = Custom(block = { r -> r.block() }, weight = weight),
            evidence = { c -> c as? R },
        )
    }
}

/** A rule can use only descriptors declared in its namespace. */
internal fun Targeting<*>.usesDeclaredFacts(schema: FactSchema): Boolean = when (this) {
    is Targeting.Facts -> expression in schema
    is Targeting.All<*> -> targets.all { it.usesDeclaredFacts(schema) }
    is Targeting.AnyOf<*> -> targets.all { it.usesDeclaredFacts(schema) }
    is Targeting.Guarded<*, *> -> inner.usesDeclaredFacts(schema)
    is Targeting.Custom<*> -> true
}
