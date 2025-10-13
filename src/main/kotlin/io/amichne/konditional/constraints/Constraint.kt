package io.amichne.konditional.constraints

import io.amichne.konditional.core.rules.Rule

interface Constraint {
    fun matches(
        rule: Rule
    ): Boolean
}
