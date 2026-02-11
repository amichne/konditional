package io.amichne.kontracts.schema

data class RootObjectSchema(
    override val fields: Map<String, FieldSchema>,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Map<String, Any?>? = null,
    override val nullable: Boolean = false,
    override val example: Map<String, Any?>? = null,
    override val deprecated: Boolean = false,
    override val required: Set<String>? = null
) : JsonSchema<Map<String, Any?>>(), ObjectTraits {
    override val type: OpenApi.Type = OpenApi.Type.OBJECT
    override fun toString() = "RootObjectSchema(fields=${fields.keys})"

    internal constructor(objectSchema: ObjectSchema) : this(
        fields = objectSchema.fields,
        title = objectSchema.title,
        description = objectSchema.description,
        default = objectSchema.default,
        nullable = objectSchema.nullable,
        example = objectSchema.example,
        deprecated = objectSchema.deprecated,
        required = objectSchema.required
    )
}
