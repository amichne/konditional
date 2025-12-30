package io.amichne.konditional.configstate

data class SemverConstraintsDescriptor(
    override val uiHints: UiHints,
    val minimum: String,
    val allowAnyAboveMinimum: Boolean = true,
    val pattern: String? = null,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.SEMVER_CONSTRAINTS
}
