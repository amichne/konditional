package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class StringConstraintsDescriptor(
    override val uiHints: UiHints,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val suggestions: List<String>? = null,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.STRING_CONSTRAINTS
}
