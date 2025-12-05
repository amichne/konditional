package io.amichne.konditional.rules.evaluable

import io.amichne.konditional.kontext.Kontext

object Placeholder : Evaluable<Kontext<*>> {
    override fun matches(kontext: Kontext<*>): Boolean = true

    override fun specificity(): Int = 0
}
