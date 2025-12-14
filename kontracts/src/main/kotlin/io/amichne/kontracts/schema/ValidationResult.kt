package io.amichne.kontracts.schema

import kotlin.reflect.KClass

/**
 * Result of schema validation.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid

    fun getErrorMessage(): String? = when (this) {
        is Invalid -> message
        is Valid -> null
    }

    companion object {
        inline fun <reified T : JsonSchema> KClass<T>.typeCheck(schema: JsonSchema): ValidationResult =
            if (isInstance(schema)) Valid else Invalid("Expected type ${T::class.simpleName}, but got $simpleName")
    }
}
