package io.amichne.kontracts.schema

data class RootObjectSchema(
    override val fields: Map<String, FieldSchema>,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false,
    override val required: Set<String>? = null
) : JsonSchema(), ObjectTraits {
    override fun toString() = "RootObjectSchema(fields=${fields.keys})"
}
