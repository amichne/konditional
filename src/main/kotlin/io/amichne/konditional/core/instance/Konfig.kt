package io.amichne.konditional.core.instance

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag

@ConsistentCopyVisibility
data class Konfig internal constructor(val flags: Map<Conditional<*, *, *>, FeatureFlag<*, *, *>>)
