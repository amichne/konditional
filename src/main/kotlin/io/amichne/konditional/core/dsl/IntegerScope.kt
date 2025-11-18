package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue

@KonditionalDsl
interface IntegerScope<C : Context, M : Namespace> : FlagScope<EncodableValue.IntEncodeable, Int, C, M>
