package io.amichne.kontracts.schema

interface ObjectTraits {
    val fields: Map<String, FieldSchema>
    val required: Set<String>?

    /**
     * Validates that all required fields are present.
     */
    fun validateRequiredFields(fieldNames: Set<String>): ValidationResult {
        val req = required ?: fields.filter { it.value.required }.keys
        val missing = req - fieldNames
        return if (missing.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Missing required fields: $missing")
        }
    }
}
