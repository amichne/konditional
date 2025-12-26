package io.amichne.konditional.configstate

data class BooleanDescriptor(
    override val uiHints: UiHints,
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.BOOLEAN,
) : FieldDescriptor
