package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.types.EncodableValue

@KonditionalDsl
interface StringScope<C : Context, M : Namespace> : FlagScope<EncodableValue.StringEncodeable, String, C, M>
