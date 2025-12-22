package io.amichne.kontracts.value

import io.amichne.kontracts.schema.BooleanSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ValidationResult
import io.amichne.kontracts.schema.ValidationResult.Companion.typeCheck

/**
 * JSON boolean value.
 */
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun validate(schema: JsonSchema<*>): ValidationResult = BooleanSchema::class.typeCheck(schema)

    override fun toString() = value.toString()
}
