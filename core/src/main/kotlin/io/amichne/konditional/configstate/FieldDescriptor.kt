package io.amichne.konditional.configstate

sealed interface FieldDescriptor {
    val kind: Kind
    val uiHints: UiHints

    enum class Kind {
        BOOLEAN,
        ENUM_OPTIONS,
        NUMBER_RANGE,
        SEMVER_CONSTRAINTS,
        STRING_CONSTRAINTS,
        SCHEMA_REF,
        MAP_CONSTRAINTS,
    }
}
