package io.amichne.konditional.core.dsl

import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.StringEncodeable
import io.amichne.konditional.kontext.Kontext

@KonditionalDsl
interface StringScope<C : Kontext<M>, M : Namespace> : FlagScope<StringEncodeable, String, C, M>
