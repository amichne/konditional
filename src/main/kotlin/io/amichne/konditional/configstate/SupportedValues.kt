package io.amichne.konditional.configstate

/**
 * Mutation metadata payload returned alongside the current state.
 *
 * - [bindings] maps a JSON Pointer template to a [FieldType], allowing clients to bind any
 *   modifiable field in [ConfigurationStateResponse.currentState] to a set of supported values.
 * - [byType] maps a [FieldType] to its [FieldDescriptor] (including UI hints) describing
 *   valid values or constraints.
 */
data class SupportedValues(
    val bindings: Map<String, FieldType>,
    val byType: Map<String, FieldDescriptor>,
)
