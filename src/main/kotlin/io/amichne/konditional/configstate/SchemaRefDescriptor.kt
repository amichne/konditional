package io.amichne.konditional.configstate

data class SchemaRefDescriptor(
    override val uiHints: UiHints,
    val ref: String,
    override val kind: FieldDescriptor.Kind = FieldDescriptor.Kind.SCHEMA_REF,
) : FieldDescriptor
