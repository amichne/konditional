package io.amichne.konditional.configmetadata.contract

import io.amichne.konditional.configmetadata.descriptor.ValueDescriptor

/**
 * Metadata describing how to interpret and mutate configuration payloads.
 *
 * - [bindings] maps JSON Pointer templates to [BindingType] identifiers.
 * - [descriptors] declares the UI and validation constraints for each [BindingType].
 */
data class ConfigMetadata(
    val bindings: Map<String, BindingType>,
    val descriptors: Map<BindingType, ValueDescriptor>,
) {
    companion object {
        val empty: ConfigMetadata = ConfigMetadata(emptyMap(), emptyMap())
    }
}
