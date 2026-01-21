package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

data object Placeholder : Predicate<Context> {
    override fun matches(context: Context): Boolean = true

    override fun specificity(): Int = 0
}
