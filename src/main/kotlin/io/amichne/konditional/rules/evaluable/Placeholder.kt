package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

interface Placeholder<C : Context> : Evaluable<C> {
    override fun matches(context: C): Boolean = true

    override fun specificity(): Int = 0

    companion object {
        operator fun <C : Context> invoke(): Evaluable<C> = object : Evaluable<C> {
            override fun matches(context: C): Boolean = true
            override fun specificity(): Int = 0
        }
    }
}
