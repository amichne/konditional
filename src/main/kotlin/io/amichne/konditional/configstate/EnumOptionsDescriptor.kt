package io.amichne.konditional.configstate

data class EnumOptionsDescriptor(
    override val uiHints: UiHints,
    val options: List<Option>,
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.ENUM_OPTIONS,
) : FieldDescriptor
