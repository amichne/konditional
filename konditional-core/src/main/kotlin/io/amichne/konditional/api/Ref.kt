package io.amichne.konditional.api

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature

/**
 * Typed reference to a feature evaluation that can be composed without runtime indirection.
 *
 * References are deterministic: for a fixed context and configuration snapshot, evaluation is
 * referentially transparent and depends only on the referenced features.
 */
sealed interface Ref<T : Any, C : Context> {
    /**
     * Evaluate this reference with the provided context.
     */
    fun evaluate(context: C): CompositeEvaluationResult<T>

    /**
     * Transform the evaluated value while preserving dependency metadata.
     */
    fun <R : Any> map(transform: (T, C) -> R): Ref<R, C> = MappedRef(this, transform)

    /**
     * Chain a dependent reference using the value of this reference.
     */
    fun <R : Any> flatMap(transform: (T, C) -> Ref<R, C>): Ref<R, C> = FlatMappedRef(this, transform)

    /**
     * Alias for [flatMap] to support fluent chaining.
     */
    fun <R : Any> thenUse(transform: (T, C) -> Ref<R, C>): Ref<R, C> = flatMap(transform)

    /**
     * Combine two references into a single reference yielding a pair.
     */
    fun <R : Any> zip(other: Ref<R, C>): Ref<Pair<T, R>, C> = ZippedRef(this, other)

    /**
     * Adapt this reference to a different context by projecting the required context.
     */
    fun <D : Context> contraMapContext(transform: (D) -> C): Ref<T, D> = ContraMappedRef(this, transform)
}

/**
 * Create a typed reference from a feature definition.
 */
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.bind(): Ref<T, C> = FeatureRef(this)

@PublishedApi
internal fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.toRefInternal(): Ref<T, C> =
    FeatureRef(this)

/**
 * Creates a typed reference from a feature definition without explicit bind calls.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace> Feature<T, C, M>.asRef(): Ref<T, C> =
    toRefInternal()

/**
 * Transform a feature value using the context without needing an explicit reference.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, R : Any> Feature<T, C, M>.map(
    noinline transform: (T, C) -> R,
): Ref<R, C> = toRefInternal().map(transform)

/**
 * Chain feature evaluation without explicit reference construction.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, R : Any> Feature<T, C, M>.flatMap(
    noinline transform: (T, C) -> Ref<R, C>,
): Ref<R, C> = toRefInternal().flatMap(transform)

/**
 * Alias for [flatMap] to support fluent chaining from features.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, R : Any> Feature<T, C, M>.thenUse(
    noinline transform: (T, C) -> Ref<R, C>,
): Ref<R, C> = toRefInternal().thenUse(transform)

/**
 * Combine two features into a composed reference without explicit binding.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, R : Any> Feature<T, C, M>.zip(
    other: Feature<R, C, *>,
): Ref<Pair<T, R>, C> = toRefInternal().zip(other.toRefInternal())

/**
 * Combine a feature with an existing reference.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, R : Any> Feature<T, C, M>.zip(
    other: Ref<R, C>,
): Ref<Pair<T, R>, C> = toRefInternal().zip(other)

/**
 * Adapt a feature reference to a different context by projecting the required context.
 */
inline fun <reified T : Any, reified C : Context, M : Namespace, D : Context> Feature<T, C, M>.contraMapContext(
    noinline transform: (D) -> C,
): Ref<T, D> = toRefInternal().contraMapContext(transform)

private data class FeatureRef<T : Any, C : Context, M : Namespace>(
    val feature: Feature<T, C, M>,
) : Ref<T, C> {
    override fun evaluate(context: C): CompositeEvaluationResult<T> = run {
        val result = feature.explain(context)
        CompositeEvaluationResult(value = result.value, dependencies = listOf(result))
    }
}

private data class MappedRef<T : Any, R : Any, C : Context>(
    val source: Ref<T, C>,
    val transform: (T, C) -> R,
) : Ref<R, C> {
    override fun evaluate(context: C): CompositeEvaluationResult<R> = run {
        val sourceResult = source.evaluate(context)
        val mappedValue = transform(sourceResult.value, context)
        CompositeEvaluationResult(value = mappedValue, dependencies = sourceResult.dependencies)
    }
}

private data class FlatMappedRef<T : Any, R : Any, C : Context>(
    val source: Ref<T, C>,
    val transform: (T, C) -> Ref<R, C>,
) : Ref<R, C> {
    override fun evaluate(context: C): CompositeEvaluationResult<R> = run {
        val sourceResult = source.evaluate(context)
        val next = transform(sourceResult.value, context)
        val nextResult = next.evaluate(context)
        CompositeEvaluationResult(
            value = nextResult.value,
            dependencies = sourceResult.dependencies + nextResult.dependencies,
        )
    }
}

private data class ZippedRef<A : Any, B : Any, C : Context>(
    val left: Ref<A, C>,
    val right: Ref<B, C>,
) : Ref<Pair<A, B>, C> {
    override fun evaluate(context: C): CompositeEvaluationResult<Pair<A, B>> = run {
        val leftResult = left.evaluate(context)
        val rightResult = right.evaluate(context)
        CompositeEvaluationResult(
            value = leftResult.value to rightResult.value,
            dependencies = leftResult.dependencies + rightResult.dependencies,
        )
    }
}

private data class ContraMappedRef<T : Any, C : Context, D : Context>(
    val source: Ref<T, C>,
    val transform: (D) -> C,
) : Ref<T, D> {
    override fun evaluate(context: D): CompositeEvaluationResult<T> = source.evaluate(transform(context))
}
