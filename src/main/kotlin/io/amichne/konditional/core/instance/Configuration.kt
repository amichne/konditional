package io.amichne.konditional.core.instance

import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature

@ConsistentCopyVisibility
data class Configuration internal constructor(val flags: Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>)
