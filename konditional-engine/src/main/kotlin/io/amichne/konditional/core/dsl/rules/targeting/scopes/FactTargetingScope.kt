package io.amichne.konditional.core.dsl.rules.targeting.scopes

import io.amichne.konditional.context.fact.FactExpression
import io.amichne.konditional.core.dsl.KonditionalDsl

/** Accepts expressions over the namespace's declared facts. */
@KonditionalDsl
interface FactTargetingScope {
    /** Adds [expression] as one condition in the enclosing conjunction or disjunction. */
    fun match(expression: FactExpression)
}
