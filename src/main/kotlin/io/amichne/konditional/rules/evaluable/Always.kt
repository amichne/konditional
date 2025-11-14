package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.context.Context

object Always : Evaluable<Context> {
    override fun matches(context: Context): Boolean = true
}
