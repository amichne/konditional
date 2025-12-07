package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

@KonditionalDsl
interface IntegerScope<C : Context, M : Namespace> : FlagScope< Int, C, M>
