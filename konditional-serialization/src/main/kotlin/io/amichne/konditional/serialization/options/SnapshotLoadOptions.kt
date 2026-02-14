package io.amichne.konditional.serialization.options

/**
 * Controls deserialization behavior for snapshots and patches.
 *
 * Default behavior is strict and namespace-scoped:
 * - feature resolution requires an explicit trusted feature index
 * - unknown feature keys fail the entire load
 *
 * Operationally, some deployments prefer forward-compatibility during migrations; in that case,
 * unknown keys can be skipped while emitting warnings.
 */
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val onWarning: (SnapshotWarning) -> Unit = {},
) {
    companion object {
        /**
         * Strict/default mode: explicit feature scope required.
         */
        fun strict(): SnapshotLoadOptions =
            SnapshotLoadOptions(
                unknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
            )

        /**
         * Strict mode with unknown feature keys skipped and surfaced through [onWarning].
         */
        fun skipUnknownKeys(onWarning: (SnapshotWarning) -> Unit = {}): SnapshotLoadOptions =
            SnapshotLoadOptions(
                unknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Skip,
                onWarning = onWarning,
            )
    }
}
