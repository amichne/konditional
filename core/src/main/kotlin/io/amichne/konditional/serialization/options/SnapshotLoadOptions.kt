package io.amichne.konditional.serialization.options

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
