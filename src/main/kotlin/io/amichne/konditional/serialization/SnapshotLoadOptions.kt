package io.amichne.konditional.serialization

import io.amichne.konditional.values.Identifier

/**
 * Controls deserialization behavior for snapshots and patches.
 *
 * Default behavior is strict: any unknown feature key fails the entire load.
 * Operationally, some deployments prefer forward-compatibility during migrations; in that case,
 * unknown keys can be skipped while emitting warnings.
 */
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val onWarning: (SnapshotWarning) -> Unit = {},
) {
    companion object {
        fun strict(): SnapshotLoadOptions = SnapshotLoadOptions(UnknownFeatureKeyStrategy.Fail)
        fun skipUnknownKeys(onWarning: (SnapshotWarning) -> Unit = {}): SnapshotLoadOptions =
            SnapshotLoadOptions(UnknownFeatureKeyStrategy.Skip, onWarning)
    }
}

sealed interface UnknownFeatureKeyStrategy {
    data object Fail : UnknownFeatureKeyStrategy
    data object Skip : UnknownFeatureKeyStrategy
}

@ConsistentCopyVisibility
data class SnapshotWarning internal constructor(
    val kind: Kind,
    val message: String,
    val key: Identifier? = null,
) {
    enum class Kind {
        UNKNOWN_FEATURE_KEY,
    }

    companion object {
        fun unknownFeatureKey(key: Identifier): SnapshotWarning = SnapshotWarning(
            kind = Kind.UNKNOWN_FEATURE_KEY,
            key = key,
            message = "Unknown feature key encountered during deserialization: $key",
        )
    }
}
