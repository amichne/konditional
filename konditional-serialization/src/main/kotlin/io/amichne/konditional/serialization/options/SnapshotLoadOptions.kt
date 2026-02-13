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
 *
 * Legacy global registry fallback remains available as an explicit opt-in
 * via [legacyGlobalRegistryFallback].
 */
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val onWarning: (SnapshotWarning) -> Unit = {},
    val featureResolutionMode: FeatureResolutionMode = FeatureResolutionMode.RequireExplicitFeatureScope,
) {
    /**
     * Controls how [io.amichne.konditional.values.FeatureId] keys are resolved at decode time.
     */
    sealed interface FeatureResolutionMode {
        /**
         * Requires callers to provide a trusted namespace-scoped feature index.
         * This preserves isolation and removes runtime type-hint fallback behavior.
         */
        data object RequireExplicitFeatureScope : FeatureResolutionMode

        /**
         * Legacy compatibility mode:
         * missing entries in the provided scope may fall back to [io.amichne.konditional.serialization.FeatureRegistry].
         */
        data object LegacyGlobalRegistryFallback : FeatureResolutionMode
    }

    companion object {
        /**
         * Strict/default mode: no global fallback, no implicit type-hint fallback.
         */
        fun strict(): SnapshotLoadOptions =
            SnapshotLoadOptions(
                unknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
                featureResolutionMode = FeatureResolutionMode.RequireExplicitFeatureScope,
            )

        /**
         * Strict mode with unknown feature keys skipped and surfaced through [onWarning].
         */
        fun skipUnknownKeys(onWarning: (SnapshotWarning) -> Unit = {}): SnapshotLoadOptions =
            SnapshotLoadOptions(
                unknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Skip,
                onWarning = onWarning,
                featureResolutionMode = FeatureResolutionMode.RequireExplicitFeatureScope,
            )

        /**
         * Explicit legacy compatibility mode.
         *
         * Feature keys resolve through the provided namespace scope first, then fall back to global
         * [io.amichne.konditional.serialization.FeatureRegistry] when missing.
         */
        fun legacyGlobalRegistryFallback(
            unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
            onWarning: (SnapshotWarning) -> Unit = {},
        ): SnapshotLoadOptions =
            SnapshotLoadOptions(
                unknownFeatureKeyStrategy = unknownFeatureKeyStrategy,
                onWarning = onWarning,
                featureResolutionMode = FeatureResolutionMode.LegacyGlobalRegistryFallback,
            )
    }
}
