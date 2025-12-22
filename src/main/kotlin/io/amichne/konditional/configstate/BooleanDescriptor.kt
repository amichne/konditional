package io.amichne.konditional.configstate

data class BooleanDescriptor(
    override val uiHints: UiHints,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.BOOLEAN
}
