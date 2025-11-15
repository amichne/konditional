package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

object Placeholder : Evaluable<Context> {
    override fun matches(context: Context): Boolean = true

    override fun specificity(): Int = 0
}
