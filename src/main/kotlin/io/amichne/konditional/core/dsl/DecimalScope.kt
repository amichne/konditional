package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Taxonomy
import io.amichne.konditional.core.types.EncodableValue

@FeatureFlagDsl
interface DecimalScope<C : Context, M : Taxonomy> : FlagScope<EncodableValue.DecimalEncodeable, Double, C, M>
