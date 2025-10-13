package io.amichne.konditional.constraints

import io.amichne.konditional.rules.Rule

interface Constraint {
    fun matches(
        rule: Rule
    ): Boolean
}
