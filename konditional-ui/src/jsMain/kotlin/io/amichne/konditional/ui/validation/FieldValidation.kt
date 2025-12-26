package io.amichne.konditional.ui.validation

import io.amichne.konditional.ui.json.asDoubleOrNull
import io.amichne.konditional.ui.json.asStringOrNull
import io.amichne.konditional.ui.model.ArrayDescriptorDto
import io.amichne.konditional.ui.model.BooleanDescriptorDto
import io.amichne.konditional.ui.model.EnumOptionsDescriptorDto
import io.amichne.konditional.ui.model.FieldDescriptorDto
import io.amichne.konditional.ui.model.MapConstraintsDescriptorDto
import io.amichne.konditional.ui.model.NumberRangeDescriptorDto
import io.amichne.konditional.ui.model.ObjectDescriptorDto
import io.amichne.konditional.ui.model.SchemaRefDescriptorDto
import io.amichne.konditional.ui.model.SemverConstraintsDescriptorDto
import io.amichne.konditional.ui.model.StringConstraintsDescriptorDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Severity levels for validation errors.
 */
enum class Severity {
    ERROR,
    WARNING,
    INFO,
}

/**
 * A single validation error with path and message.
 */
data class ValidationError(
    val path: String,
    val message: String,
    val severity: Severity = Severity.ERROR,
)

/**
 * Result of validating a field value.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<ValidationError>) : ValidationResult

    fun isValid(): Boolean = this is Valid

    fun errors(): List<ValidationError> =
        when (this) {
            is Valid -> emptyList()
            is Invalid -> errors
        }
}

/**
 * Combines multiple validation results into one.
 */
fun List<ValidationResult>.combine(): ValidationResult {
    val allErrors = flatMap { it.errors() }
    return if (allErrors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(allErrors)
}

/**
 * Validates a JSON value against a field descriptor.
 *
 * @param value The JSON value to validate
 * @param descriptor The descriptor defining validation rules
 * @param path The JSON pointer path for error reporting (default: root)
 * @return Validation result with any errors found
 */
fun validate(
    value: JsonElement?,
    descriptor: FieldDescriptorDto,
    path: String = "",
): ValidationResult =
    when (descriptor) {
        is BooleanDescriptorDto -> validateBoolean(value, path)
        is NumberRangeDescriptorDto -> validateNumber(value, descriptor, path)
        is EnumOptionsDescriptorDto -> validateEnum(value, descriptor, path)
        is StringConstraintsDescriptorDto -> validateString(value, descriptor, path)
        is SemverConstraintsDescriptorDto -> validateSemver(value, descriptor, path)
        is MapConstraintsDescriptorDto -> validateMap(value, descriptor, path)
        is SchemaRefDescriptorDto -> ValidationResult.Valid // Schema validation delegated elsewhere
        is ArrayDescriptorDto -> validateArray(value, descriptor, path)
        is ObjectDescriptorDto -> validateObject(value, descriptor, path)
    }

private fun validateBoolean(
    value: JsonElement?,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Invalid(listOf(ValidationError(path, "Value is required")))
    }
    val primitive = value as? JsonPrimitive
    if (primitive?.booleanOrNull == null) {
        return ValidationResult.Invalid(listOf(ValidationError(path, "Expected boolean value")))
    }
    return ValidationResult.Valid
}

private fun validateNumber(
    value: JsonElement?,
    descriptor: NumberRangeDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Invalid(listOf(ValidationError(path, "Value is required")))
    }

    val number = value.asDoubleOrNull()
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected numeric value")))

    val errors = mutableListOf<ValidationError>()

    if (number < descriptor.min) {
        errors += ValidationError(path, "Value must be at least ${descriptor.min}", Severity.ERROR)
    }
    if (number > descriptor.max) {
        errors += ValidationError(path, "Value must be at most ${descriptor.max}", Severity.ERROR)
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

private fun validateEnum(
    value: JsonElement?,
    descriptor: EnumOptionsDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Invalid(listOf(ValidationError(path, "Value is required")))
    }

    val validValues = descriptor.options.map { it.value }.toSet()

    return when (value) {
        is JsonPrimitive -> {
            val str = value.asStringOrNull()
            if (str !in validValues) {
                ValidationResult.Invalid(listOf(ValidationError(path, "Invalid value: '$str'. Must be one of: ${validValues.joinToString()}")))
            } else {
                ValidationResult.Valid
            }
        }
        is JsonArray -> {
            val errors = value.mapIndexedNotNull { idx, item ->
                val str = item.asStringOrNull()
                if (str !in validValues) {
                    ValidationError("$path/$idx", "Invalid value: '$str'")
                } else {
                    null
                }
            }
            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
        }
        else -> ValidationResult.Invalid(listOf(ValidationError(path, "Expected string or array")))
    }
}

private fun validateString(
    value: JsonElement?,
    descriptor: StringConstraintsDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Valid // Allow null for optional strings
    }

    val str = value.asStringOrNull()
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected string value")))

    val errors = mutableListOf<ValidationError>()

    descriptor.minLength?.let { min ->
        if (str.length < min) {
            errors += ValidationError(path, "Minimum length: $min (current: ${str.length})")
        }
    }

    descriptor.maxLength?.let { max ->
        if (str.length > max) {
            errors += ValidationError(path, "Maximum length: $max (current: ${str.length})")
        }
    }

    descriptor.pattern?.let { pattern ->
        if (!Regex(pattern).matches(str)) {
            errors += ValidationError(path, "Does not match pattern: $pattern")
        }
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

private fun validateSemver(
    value: JsonElement?,
    descriptor: SemverConstraintsDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Invalid(listOf(ValidationError(path, "Version is required")))
    }

    val str = value.asStringOrNull()
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected string value")))

    val errors = mutableListOf<ValidationError>()

    descriptor.pattern?.let { pattern ->
        if (!Regex(pattern).matches(str)) {
            errors += ValidationError(path, "Invalid version format")
        }
    }

    val parsed = Semver.parse(str)
    if (parsed == null) {
        errors += ValidationError(path, "Invalid semver: '$str'")
    } else {
        val minimum = Semver.parse(descriptor.minimum)
        if (minimum != null && descriptor.allowAnyAboveMinimum && parsed < minimum) {
            errors += ValidationError(path, "Version must be >= ${descriptor.minimum}")
        }
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

private fun validateMap(
    value: JsonElement?,
    descriptor: MapConstraintsDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return ValidationResult.Valid // Allow null for optional maps
    }

    val obj = value as? JsonObject
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected object value")))

    val errors = mutableListOf<ValidationError>()

    obj.forEach { (key, arrayValue) ->
        // Validate key against key constraints
        val keyResult = validateString(JsonPrimitive(key), descriptor.key, "$path/$key")
        errors += keyResult.errors()

        // Validate values array
        val values = arrayValue as? JsonArray
        values?.forEachIndexed { idx, item ->
            val itemResult = validateString(item, descriptor.values, "$path/$key/$idx")
            errors += itemResult.errors()
        }
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

private fun validateArray(
    value: JsonElement?,
    descriptor: ArrayDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return if (descriptor.minItems != null && descriptor.minItems > 0) {
            ValidationResult.Invalid(listOf(ValidationError(path, "Array is required")))
        } else {
            ValidationResult.Valid
        }
    }

    val array = value as? JsonArray
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected array value")))

    val errors = mutableListOf<ValidationError>()

    // Check min items
    descriptor.minItems?.let { min ->
        if (array.size < min) {
            errors += ValidationError(path, "Minimum $min item(s) required (current: ${array.size})")
        }
    }

    // Check max items
    descriptor.maxItems?.let { max ->
        if (array.size > max) {
            errors += ValidationError(path, "Maximum $max item(s) allowed (current: ${array.size})")
        }
    }

    // Check unique items
    if (descriptor.uniqueItems) {
        val seen = mutableSetOf<String>()
        array.forEachIndexed { idx, item ->
            val serialized = item.toString()
            if (serialized in seen) {
                errors += ValidationError("$path/$idx", "Duplicate value not allowed")
            }
            seen += serialized
        }
    }

    // Validate each item
    array.forEachIndexed { idx, item ->
        val itemResult = validate(item, descriptor.itemDescriptor, "$path/$idx")
        errors += itemResult.errors()
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

private fun validateObject(
    value: JsonElement?,
    descriptor: ObjectDescriptorDto,
    path: String,
): ValidationResult {
    if (value == null || value is JsonNull) {
        return if (descriptor.required.isNotEmpty()) {
            ValidationResult.Invalid(listOf(ValidationError(path, "Object is required")))
        } else {
            ValidationResult.Valid
        }
    }

    val obj = value as? JsonObject
        ?: return ValidationResult.Invalid(listOf(ValidationError(path, "Expected object value")))

    val errors = mutableListOf<ValidationError>()

    // Check required properties
    descriptor.required.forEach { requiredKey ->
        if (requiredKey !in obj || obj[requiredKey] is JsonNull) {
            errors += ValidationError("$path/$requiredKey", "Required field is missing")
        }
    }

    // Validate each property
    descriptor.properties.forEach { (key, propDescriptor) ->
        val propValue = obj[key]
        val propResult = validate(propValue, propDescriptor.descriptor, "$path/$key")
        errors += propResult.errors()
    }

    // Validate additional properties if present
    descriptor.additionalProperties?.let { additionalDesc ->
        val knownKeys = descriptor.properties.keys
        obj.keys.filter { it !in knownKeys }.forEach { additionalKey ->
            val additionalValue = obj[additionalKey]
            val additionalResult = validate(additionalValue, additionalDesc, "$path/$additionalKey")
            errors += additionalResult.errors()
        }
    }

    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

/**
 * Simple semver representation for validation.
 */
internal data class Semver(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<Semver> {
    override fun compareTo(other: Semver): Int =
        compareValuesBy(this, other, Semver::major, Semver::minor, Semver::patch)

    companion object {
        private val basicPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+).*$")

        fun parse(value: String): Semver? =
            basicPattern.matchEntire(value)
                ?.groupValues
                ?.let { groups ->
                    val major = groups.getOrNull(1)?.toIntOrNull()
                    val minor = groups.getOrNull(2)?.toIntOrNull()
                    val patch = groups.getOrNull(3)?.toIntOrNull()
                    if (major != null && minor != null && patch != null) {
                        Semver(major, minor, patch)
                    } else {
                        null
                    }
                }
    }
}
