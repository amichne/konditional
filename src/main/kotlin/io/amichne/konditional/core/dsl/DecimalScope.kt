package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.DecimalEncodeable
import io.amichne.konditional.kontext.Kontext

@KonditionalDsl
interface DecimalScope<C : Kontext<M>, M : Namespace> : FlagScope<DecimalEncodeable, Double, C, M>
