package io.amichne.konditional.configstate

data class EnumOptionsDescriptor(
    override val uiHints: UiHints,
    val options: List<Option>,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.ENUM_OPTIONS
}
