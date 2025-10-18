package io.amichne.konditional.rules

import io.amichne.konditional.context.Context

data class Surjection<S : Any, C : Context>(val rule: Rule<C>, val value: S)
