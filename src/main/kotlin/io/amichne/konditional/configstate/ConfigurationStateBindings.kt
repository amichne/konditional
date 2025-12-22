package io.amichne.konditional.configstate

/**
 * JSON Pointer templates that bind modifiable fields in the snapshot JSON to a [FieldType].
 *
 * Pointer templates follow RFC 6901 JSON Pointer, with the following additional convention:
 * - `*` matches any array element index (e.g., `/flags/{any}/rules/{any}/rampUp`)
 *
 * Templates are defined against the persisted snapshot JSON model (SerializableSnapshot).
 */
object ConfigurationStateBindings {
    val bindings: Map<String, FieldType> =
        mapOf(
            "/flags/*/defaultValue" to FieldType.FLAG_VALUE,
            "/flags/*/salt" to FieldType.SALT,
            "/flags/*/isActive" to FieldType.FLAG_ACTIVE,
            "/flags/*/rampUpAllowlist" to FieldType.RAMP_UP_ALLOWLIST,

            "/flags/*/rules/*/value" to FieldType.FLAG_VALUE,
            "/flags/*/rules/*/rampUp" to FieldType.RAMP_UP_PERCENT,
            "/flags/*/rules/*/rampUpAllowlist" to FieldType.RAMP_UP_ALLOWLIST,
            "/flags/*/rules/*/note" to FieldType.RULE_NOTE,
            "/flags/*/rules/*/locales" to FieldType.LOCALES,
            "/flags/*/rules/*/platforms" to FieldType.PLATFORMS,
            "/flags/*/rules/*/versionRange" to FieldType.VERSION_RANGE,
            "/flags/*/rules/*/axes" to FieldType.AXES_MAP,
        )
}
