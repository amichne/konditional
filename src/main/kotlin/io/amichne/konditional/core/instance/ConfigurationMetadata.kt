package io.amichne.konditional.core.instance

/**
 * Optional metadata attached to a [Configuration].
 *
 * This is primarily intended for operational tooling:
 * - auditability (what config version is active?)
 * - rollbacks (load a prior known-good version)
 * - debugging (where did this config come from?)
 *
 * Metadata is treated as opaque by the evaluation engine. Absence does not affect evaluation.
 *
 * @property version An optional human-assigned version identifier (git SHA, config revision, etc.)
 * @property generatedAtEpochMillis An optional epoch timestamp (ms) for when the config was generated upstream
 * @property source An optional description of the config source (service name, bucket key, file path, etc.)
 */
@ConsistentCopyVisibility
data class ConfigurationMetadata internal constructor(
    val version: String? = null,
    val generatedAtEpochMillis: Long? = null,
    val source: String? = null,
) {
    companion object {
        fun of(
            version: String? = null,
            generatedAtEpochMillis: Long? = null,
            source: String? = null,
        ): ConfigurationMetadata = ConfigurationMetadata(
            version = version,
            generatedAtEpochMillis = generatedAtEpochMillis,
            source = source,
        )
    }
}

