package io.amichne.kontracts.value

import io.amichne.kontracts.schema.DoubleSchema
import io.amichne.kontracts.schema.IntSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ValidationResult

/**
 * JSON number value (stored as Double for precision).
 */
data class JsonNumber(val value: Double) : JsonValue() {
    override fun validate(schema: JsonSchema): ValidationResult {
        return when (schema) {
            is IntSchema -> {
                if (value == value.toInt().toDouble()) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(
                        "Expected integer, but got non-integer number: $value"
                    )
                }
            }
            is DoubleSchema -> ValidationResult.Valid
            else -> ValidationResult.Invalid(
                "Expected ${schema}, but got Number"
            )
        }
    }

    fun toInt(): Int = value.toInt()
    fun toDouble(): Double = value

    override fun toString() = value.toString()
}
