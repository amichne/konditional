package io.amichne.konditional.core.snapshot

import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.ContextualFeatureFlag
import io.amichne.konditional.serialization.SnapshotSerializer

@ConsistentCopyVisibility
data class Snapshot internal constructor(
    val flags: Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>,
) {
    companion object {

        fun fromJson(json: String, serializer: SnapshotSerializer = SnapshotSerializer.default): Snapshot =
            serializer.deserialize(json)
    }
}
