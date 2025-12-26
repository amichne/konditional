package io.amichne.konditional.configstate.ui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ConfigurationStateResponseDto(
    val currentState: JsonElement,
    val supportedValues: SupportedValuesDto,
)

@Serializable
data class SupportedValuesDto(
    val bindings: Map<String, FieldTypeDto>,
    val byType: Map<String, FieldDescriptorDto>,
)

@Serializable
enum class FieldTypeDto {
    FLAG_VALUE,
    FLAG_ACTIVE,
    SALT,
    RAMP_UP_PERCENT,
    RAMP_UP_ALLOWLIST,
    LOCALES,
    PLATFORMS,
    SEMVER,
    VERSION_RANGE,
    AXES_MAP,
    RULE_NOTE,
}

@Serializable
enum class UiControlTypeDto {
    TOGGLE,
    TEXT,
    TEXTAREA,
    NUMBER,
    PERCENT,
    SELECT,
    MULTISELECT,
    KEY_VALUE,
    JSON,
    SEMVER,
    SEMVER_RANGE,
}

@Serializable
data class UiHintsDto(
    val control: UiControlTypeDto,
    val label: String? = null,
    val helpText: String? = null,
    val placeholder: String? = null,
    val advanced: Boolean = false,
    val order: Int? = null,
)

@Serializable
data class OptionDto(
    val value: String,
    val label: String,
)

/**
 * Client-side mirror of the server-side configstate descriptor hierarchy.
 *
 * Decoding uses `kind` as a discriminator (see `Json { classDiscriminator = "kind" }`).
 */
@Serializable
sealed interface FieldDescriptorDto {
    val uiHints: UiHintsDto
}

@Serializable
@SerialName("BOOLEAN")
data class BooleanDescriptorDto(
    override val uiHints: UiHintsDto,
) : FieldDescriptorDto

@Serializable
@SerialName("ENUM_OPTIONS")
data class EnumOptionsDescriptorDto(
    override val uiHints: UiHintsDto,
    val options: List<OptionDto>,
) : FieldDescriptorDto

@Serializable
@SerialName("NUMBER_RANGE")
data class NumberRangeDescriptorDto(
    override val uiHints: UiHintsDto,
    val min: Double,
    val max: Double,
    val step: Double,
    val unit: String? = null,
) : FieldDescriptorDto

@Serializable
@SerialName("SEMVER_CONSTRAINTS")
data class SemverConstraintsDescriptorDto(
    override val uiHints: UiHintsDto,
    val minimum: String,
    val allowAnyAboveMinimum: Boolean = true,
    val pattern: String? = null,
) : FieldDescriptorDto

@Serializable
@SerialName("STRING_CONSTRAINTS")
data class StringConstraintsDescriptorDto(
    override val uiHints: UiHintsDto,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val suggestions: List<String>? = null,
) : FieldDescriptorDto

@Serializable
@SerialName("SCHEMA_REF")
data class SchemaRefDescriptorDto(
    override val uiHints: UiHintsDto,
    val ref: String,
) : FieldDescriptorDto

@Serializable
@SerialName("MAP_CONSTRAINTS")
data class MapConstraintsDescriptorDto(
    override val uiHints: UiHintsDto,
    val key: StringConstraintsDescriptorDto,
    val values: StringConstraintsDescriptorDto,
) : FieldDescriptorDto

