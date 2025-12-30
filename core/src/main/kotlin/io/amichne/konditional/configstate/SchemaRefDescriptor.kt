package io.amichne.konditional.configstate

data class SchemaRefDescriptor(
    override val uiHints: UiHints,
    val ref: String,
) : FieldDescriptor {
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.SCHEMA_REF
}
