package io.amichne.konditional.configstate

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform

object ConfigurationStateSupportedValuesCatalog {
    const val MIN_SUPPORTED_SEMVER: String = "0.0.0"

    private const val SEMVER_PATTERN: String =
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
            "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
            "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"

    fun current(): SupportedValues =
        SupportedValues(
            bindings = ConfigurationStateBindings.bindings,
            byType = byType().mapKeys { (fieldType, _) -> fieldType.name },
        )

    private fun byType(): Map<FieldType, FieldDescriptor> =
        mapOf(
            FieldType.FLAG_VALUE to
                SchemaRefDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.JSON,
                            label = "Value",
                            helpText = "Tagged union; edit according to the value type.",
                            advanced = false,
                            order = 0,
                        ),
                    ref = "FlagValue",
                ),
            FieldType.FLAG_ACTIVE to
                BooleanDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.TOGGLE,
                            label = "Active",
                            helpText = "Disables the flag when false (default value is still kept).",
                            order = 1,
                        ),
                ),
            FieldType.SALT to
                StringConstraintsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.TEXT,
                            label = "Salt",
                            helpText = "Changes deterministic bucketing; keep stable unless intentionally re-bucketing.",
                            placeholder = "v1",
                            advanced = true,
                            order = 2,
                        ),
                    minLength = 1,
                    maxLength = 64,
                    pattern = "^[A-Za-z0-9_-]+$",
                ),
            FieldType.RAMP_UP_PERCENT to
                NumberRangeDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.PERCENT,
                            label = "Ramp-up",
                            helpText = "Percentage of eligible traffic to receive the rule value.",
                            order = 3,
                        ),
                    min = 0.0,
                    max = 100.0,
                    step = 0.1,
                    unit = "%",
                ),
            FieldType.RAMP_UP_ALLOWLIST to
                StringConstraintsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.MULTISELECT,
                            label = "Ramp-up allowlist",
                            helpText = "Identifiers that always receive the rule value regardless of ramp-up.",
                            advanced = true,
                            order = 4,
                        ),
                    minLength = 1,
                    maxLength = 128,
                    pattern = "^[0-9a-fA-F]+$",
                ),
            FieldType.LOCALES to
                EnumOptionsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.MULTISELECT,
                            label = "Locales",
                            helpText = "Restricts rule to specific locales (empty means no restriction).",
                            order = 5,
                        ),
                    options = locales(),
                ),
            FieldType.PLATFORMS to
                EnumOptionsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.MULTISELECT,
                            label = "Platforms",
                            helpText = "Restricts rule to specific platforms (empty means no restriction).",
                            order = 6,
                        ),
                    options = platforms(),
                ),
            FieldType.SEMVER to semverConstraints(),
            FieldType.VERSION_RANGE to
                SchemaRefDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.SEMVER_RANGE,
                            label = "Version range",
                            helpText = "Targets app versions; version values must satisfy the SemVer constraints.",
                            order = 7,
                        ),
                    ref = "VersionRange",
                ),
            FieldType.AXES_MAP to
                MapConstraintsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.KEY_VALUE,
                            label = "Axes",
                            helpText = "Key-value targeting constraints (axisId → allowed value IDs).",
                            advanced = true,
                            order = 8,
                        ),
                    key =
                        StringConstraintsDescriptor(
                            uiHints =
                                UiHints(
                                    control = UiControlType.TEXT,
                                    label = "Axis ID",
                                    placeholder = "environment",
                                ),
                            minLength = 1,
                            maxLength = 128,
                            pattern = "^[A-Za-z0-9_.-]+$",
                        ),
                    values =
                        StringConstraintsDescriptor(
                            uiHints =
                                UiHints(
                                    control = UiControlType.TEXT,
                                    label = "Allowed ID",
                                    placeholder = "prod",
                                ),
                            minLength = 1,
                            maxLength = 128,
                            pattern = "^[A-Za-z0-9_.-]+$",
                        ),
                ),
            FieldType.RULE_NOTE to
                StringConstraintsDescriptor(
                    uiHints =
                        UiHints(
                            control = UiControlType.TEXTAREA,
                            label = "Note",
                            helpText = "Human-readable note for this rule.",
                            advanced = true,
                            order = 9,
                        ),
                    maxLength = 1024,
                ),
        )

    fun semverConstraints(): SemverConstraintsDescriptor =
        SemverConstraintsDescriptor(
            uiHints =
                UiHints(
                    control = UiControlType.SEMVER,
                    label = "SemVer",
                    helpText = "Any valid semver >= minimum supported.",
                ),
            minimum = MIN_SUPPORTED_SEMVER,
            allowAnyAboveMinimum = true,
            pattern = SEMVER_PATTERN,
        )

    private fun locales(): List<Option> = AppLocale.entries.map(::localeOption)

    private fun localeOption(locale: AppLocale): Option =
        when (locale) {
            AppLocale.AUSTRALIA -> option(locale)
            AppLocale.AUSTRIA -> option(locale)
            AppLocale.BELGIUM_DUTCH -> option(locale)
            AppLocale.BELGIUM_FRENCH -> option(locale)
            AppLocale.CANADA -> option(locale)
            AppLocale.CANADA_FRENCH -> option(locale)
            AppLocale.FINLAND -> option(locale)
            AppLocale.FRANCE -> option(locale)
            AppLocale.GERMANY -> option(locale)
            AppLocale.HONG_KONG -> option(locale)
            AppLocale.HONG_KONG_ENGLISH -> option(locale)
            AppLocale.INDIA -> option(locale)
            AppLocale.ITALY -> option(locale)
            AppLocale.JAPAN -> option(locale)
            AppLocale.MEXICO -> option(locale)
            AppLocale.NETHERLANDS -> option(locale)
            AppLocale.NEW_ZEALAND -> option(locale)
            AppLocale.NORWAY -> option(locale)
            AppLocale.SINGAPORE -> option(locale)
            AppLocale.SPAIN -> option(locale)
            AppLocale.SWEDEN -> option(locale)
            AppLocale.TAIWAN -> option(locale)
            AppLocale.UNITED_KINGDOM -> option(locale)
            AppLocale.UNITED_STATES -> option(locale)
            AppLocale.ICC_EN_EU -> option(locale)
            AppLocale.ICC_EN_EI -> option(locale)
        }

    private fun platforms(): List<Option> = Platform.entries.map(::platformOption)

    private fun platformOption(platform: Platform): Option =
        when (platform) {
            Platform.IOS -> option(platform)
            Platform.ANDROID -> option(platform)
            Platform.WEB -> option(platform)
        }

    private fun option(locale: AppLocale): Option = Option(value = locale.id, label = locale.id)

    private fun option(platform: Platform): Option = Option(value = platform.id, label = platform.id)
}
