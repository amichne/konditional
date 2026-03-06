package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.rules.predicate.PredicateExpression
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.NamespaceId
import io.amichne.konditional.values.PredicateId

internal data class CompiledPredicateExpression<C : Context>(
    val targeting: Targeting<C>,
    val predicateRefs: List<PredicateRef>,
)

internal class PredicateExpressionCompiler<C : Context>(
    private val namespaceId: NamespaceId? = null,
    private val predicateRegistrar: ((PredicateRef.Registered, Targeting.Custom<C>) -> Unit)? = null,
    private val inlinePredicateIdFactory: (() -> PredicateId)? = null,
) {
    fun compile(expression: PredicateExpression<C>): CompiledPredicateExpression<C> =
        when (val node = expression.node) {
            is PredicateExpression.Node.Inline -> compileInline(node.block)
            is PredicateExpression.Node.Named ->
                CompiledPredicateExpression(
                    targeting = node.predicate.targeting,
                    predicateRefs = listOf(node.predicate.ref),
                )
            is PredicateExpression.Node.Not -> compile(node.expression).let { compiled ->
                CompiledPredicateExpression(
                    targeting = Targeting.not(compiled.targeting),
                    predicateRefs = compiled.predicateRefs,
                )
            }
            is PredicateExpression.Node.All ->
                compileGroup(node.expressions) { targets -> Targeting.allOf(targets) }
            is PredicateExpression.Node.AnyOf ->
                compileGroup(node.expressions) { targets -> Targeting.anyOf(targets) }
        }

    private fun compileInline(block: C.() -> Boolean): CompiledPredicateExpression<C> {
        val inlinePredicate = Targeting.Custom<C>(block = { context -> context.block() })
        if (!canRegisterInlinePredicate()) {
            return CompiledPredicateExpression(
                targeting = inlinePredicate,
                predicateRefs = emptyList(),
            )
        }

        val ref = PredicateRef.Registered(
            namespaceId = checkNotNull(namespaceId),
            id = checkNotNull(inlinePredicateIdFactory).invoke(),
        )
        checkNotNull(predicateRegistrar).invoke(ref, inlinePredicate)
        return CompiledPredicateExpression(
            targeting = inlinePredicate,
            predicateRefs = listOf(ref),
        )
    }

    private fun compileGroup(
        expressions: List<PredicateExpression<C>>,
        combiner: (List<Targeting<C>>) -> Targeting<C>,
    ): CompiledPredicateExpression<C> {
        val compiled = expressions.map(::compile)
        return CompiledPredicateExpression(
            targeting = combiner(compiled.map { it.targeting }),
            predicateRefs = compiled.flatMap { it.predicateRefs },
        )
    }

    private fun canRegisterInlinePredicate(): Boolean =
        namespaceId != null && predicateRegistrar != null && inlinePredicateIdFactory != null
}
