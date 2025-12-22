package io.amichne.konditional.configstate

data class StringConstraintsDescriptor(
    override val uiHints: UiHints,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val suggestions: List<String>? = null,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.STRING_CONSTRAINTS
}
