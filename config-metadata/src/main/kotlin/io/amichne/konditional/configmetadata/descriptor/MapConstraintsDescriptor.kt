package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class MapConstraintsDescriptor(
    override val uiHints: UiHints,
    val key: StringConstraintsDescriptor,
    val values: StringConstraintsDescriptor,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.MAP_CONSTRAINTS
}
