package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

sealed interface ValueDescriptor {
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
