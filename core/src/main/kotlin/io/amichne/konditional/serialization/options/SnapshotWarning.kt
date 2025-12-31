package io.amichne.konditional.serialization.options

import io.amichne.konditional.values.FeatureId

@ConsistentCopyVisibility
data class SnapshotWarning internal constructor(
    val kind: Kind,
    val message: String,
    val key: FeatureId? = null,
) {
    enum class Kind {
        UNKNOWN_FEATURE_KEY,
    }

    companion object {
        fun unknownFeatureKey(key: FeatureId): SnapshotWarning =
            SnapshotWarning(
                kind = Kind.UNKNOWN_FEATURE_KEY,
                key = key,
                message = "Unknown feature key encountered during deserialization: $key",
            )
    }
}
