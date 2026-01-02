package io.amichne.konditional.configmetadata.descriptor

import io.amichne.konditional.configmetadata.ui.UiHints

data class SemverConstraintsDescriptor(
    override val uiHints: UiHints,
    val minimum: String,
    val allowAnyAboveMinimum: Boolean = true,
    val pattern: String? = null,
) : ValueDescriptor {
    override val kind: ValueDescriptor.Kind = ValueDescriptor.Kind.SEMVER_CONSTRAINTS
}
