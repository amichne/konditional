package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.BooleanEncodeable
import io.amichne.konditional.kontext.Kontext

@KonditionalDsl
interface BooleanScope<C : Kontext<M>, M : Namespace> : FlagScope<BooleanEncodeable, Boolean, C, M>
