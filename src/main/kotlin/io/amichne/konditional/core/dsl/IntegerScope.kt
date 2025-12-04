package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.IntEncodeable

@KonditionalDsl
interface IntegerScope<C : Context, M : Namespace> : FlagScope<IntEncodeable, Int, C, M>
