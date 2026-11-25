@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.internal.builders

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.fact.FactExpression
import io.amichne.konditional.context.fact.anyOf
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.rules.targeting.scopes.AnyOfScope
import io.amichne.konditional.core.dsl.rules.targeting.scopes.NarrowingTargetingScope
import io.amichne.konditional.rules.targeting.Targeting

/**
 * Internal builder for [Targeting.AnyOf] nodes.
 *
 * Groups of facts form a portable disjunction. Groups with Kotlin predicates execute locally.
 */
@KonditionalDsl
@PublishedApi
@Suppress("OVERRIDE_DEPRECATION")
internal class AnyOfBuilder<C : Context> : AnyOfScope<C>, NarrowingTargetingScope<C> {

    private val leaves = mutableListOf<Targeting<C>>()

    override fun match(expression: FactExpression) {
        leaves += Targeting.Facts(expression)
    }

    override fun extension(block: C.() -> Boolean) {
        leaves += Targeting.Custom(block = { c -> c.block() })
    }

    override fun require(block: C.() -> Boolean) {
        extension(block)
    }

    override fun <R : Context> extensionNarrowed(
        evidence: (C) -> R?,
        block: R.() -> Boolean,
    ) {
        leaves += Targeting.Guarded(
            inner = Targeting.Custom(block = { narrowed: R -> narrowed.block() }),
            evidence = evidence,
        )
    }

    internal fun build(): Targeting<C> {
        val expressions = leaves.filterIsInstance<Targeting.Facts>().map { it.expression }
        return if (expressions.size == leaves.size) {
            Targeting.Facts(anyOf(*expressions.toTypedArray()))
        } else {
            Targeting.AnyOf(leaves.toList())
        }
    }
}
