package io.amichne.kontracts.value

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.NullSchema
import io.amichne.kontracts.schema.ValidationResult
import io.amichne.kontracts.schema.ValidationResult.Companion.typeCheck

/**
 * JSON null value.
 */
object JsonNull : JsonValue() {
    override fun validate(schema: JsonSchema<*>): ValidationResult = NullSchema::class.typeCheck(schema)

    override fun toString() = "null"
}
