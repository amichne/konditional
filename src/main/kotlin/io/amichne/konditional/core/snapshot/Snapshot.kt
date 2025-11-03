package io.amichne.konditional.core.snapshot

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.ContextualFeatureFlag

@ConsistentCopyVisibility
data class Snapshot internal constructor(
    val flags: Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>,
)
