package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.IntEncodeable
import io.amichne.konditional.kontext.Kontext

@KonditionalDsl
interface IntegerScope<C : Kontext<M>, M : Namespace> : FlagScope<IntEncodeable, Int, C, M>
