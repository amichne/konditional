package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class EnumOptionsDescriptor(
    override val uiHints: UiHints,
    val options: List<EnumOption>,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.ENUM_OPTIONS
}
