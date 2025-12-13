package io.amichne.kontracts.value

import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ValidationResult

/**
 * JSON boolean value.
 */
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun validate(schema: JsonSchema): ValidationResult {
        return when (schema) {
            is BooleanSchema -> ValidationResult.Valid
            else -> ValidationResult.Invalid(
                "Expected ${schema}, but got Boolean"
            )
        }
    }

    override fun toString() = value.toString()
}
