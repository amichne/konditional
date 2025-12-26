package io.amichne.konditional.configstate

data class MapConstraintsDescriptor(
    override val uiHints: UiHints,
    val key: StringConstraintsDescriptor,
    val values: StringConstraintsDescriptor,
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.MAP_CONSTRAINTS,
) : FieldDescriptor
