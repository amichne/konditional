package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.BooleanEncodeable

@KonditionalDsl
interface BooleanScope<C : Context, M : Namespace> : FlagScope<BooleanEncodeable, Boolean, C, M>
