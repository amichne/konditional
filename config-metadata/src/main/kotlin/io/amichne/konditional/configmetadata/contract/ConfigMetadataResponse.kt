package io.amichne.konditional.configmetadata.contract

/**
 * Response envelope for endpoints that return the current state plus mutation metadata.
 */
data class ConfigMetadataResponse<out S>(
    val state: S,
    val metadata: ConfigMetadata,
)
