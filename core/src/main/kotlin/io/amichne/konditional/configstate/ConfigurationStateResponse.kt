package io.amichne.konditional.configstate

import io.amichne.konditional.internal.serialization.models.SerializableSnapshot

/**
 * Response contract for a configuration state endpoint that returns:
 * - the current snapshot JSON model
 * - the complete set of valid mutation options for each modifiable field type
 */
data class ConfigurationStateResponse(
    val currentState: SerializableSnapshot,
    val supportedValues: SupportedValues,
)
