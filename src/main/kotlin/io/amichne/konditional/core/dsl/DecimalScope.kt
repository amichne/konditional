package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.DecimalEncodeable

@KonditionalDsl
interface DecimalScope<C : Context, M : Namespace> : FlagScope< Double, C, M>
