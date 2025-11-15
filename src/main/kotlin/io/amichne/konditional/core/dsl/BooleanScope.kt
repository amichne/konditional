package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

@FeatureFlagDsl
interface BooleanScope<C : Context, M : Taxonomy> : FlagScope<EncodableValue.BooleanEncodeable, Boolean, C, M>
