package io.amichne.konditional.api

/**
 * Result of a composed evaluation over multiple feature references.
 *
 * @property value The computed value produced by the composed reference.
 * @property dependencies The ordered list of underlying feature evaluations that contributed to the value.
 */
data class CompositeEvaluationResult<T : Any>(
    val value: T,
    val dependencies: List<EvaluationResult<*>>,
)
