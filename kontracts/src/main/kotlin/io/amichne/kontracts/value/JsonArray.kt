package io.amichne.kontracts.value

import io.amichne.kontracts.schema.ArraySchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ValidationResult

/**
 * JSON array value with homogeneous elements.
 *
 * @param elements List of array elements (must all match elementSchema)
 * @param elementSchema Schema for array elements
 */
data class JsonArray(
    val elements: List<JsonValue>,
    val elementSchema: JsonSchema? = null
) : JsonValue() {

    init {
        // Validate all elements against schema if provided
        elementSchema?.let { schema ->
            val result = validate(ArraySchema(schema))
            if (result.isInvalid) {
                throw IllegalArgumentException(
                    "JsonArray does not match schema: ${result.getErrorMessage()}"
                )
            }
        }
    }

    override fun validate(schema: JsonSchema): ValidationResult {
        if (schema !is ArraySchema) {
            return ValidationResult.Invalid(
                "Expected ${schema}, but got JsonArray"
            )
        }

        // Validate each element
        for ((index, element) in elements.withIndex()) {
            val elementValidation = element.validate(schema.elementSchema)
            if (elementValidation.isInvalid) {
                return ValidationResult.Invalid(
                    "Element at index $index: ${elementValidation.getErrorMessage()}"
                )
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Gets an element by index.
     */
    operator fun get(index: Int): JsonValue? = elements.getOrNull(index)

    /**
     * Returns the number of elements.
     */
    val size: Int get() = elements.size

    /**
     * Checks if the array is empty.
     */
    fun isEmpty(): Boolean = elements.isEmpty()

    /**
     * Checks if the array is not empty.
     */
    fun isNotEmpty(): Boolean = elements.isNotEmpty()

    override fun toString(): String = elements.toString()
}
