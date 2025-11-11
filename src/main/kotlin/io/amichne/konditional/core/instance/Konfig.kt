package io.amichne.konditional.core.instance

import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition

@ConsistentCopyVisibility
data class Konfig internal constructor(val flags: Map<Feature<*, *>, FlagDefinition<*, *>>)
