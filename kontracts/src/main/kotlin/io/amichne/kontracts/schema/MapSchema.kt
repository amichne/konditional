package io.amichne.kontracts.schema

/**
 * Schema for JSON objects with arbitrary string keys and uniform value schema.
 */
data class MapSchema(
    val valueSchema: JsonSchema,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false,
    val minProperties: Int? = null,
    val maxProperties: Int? = null
) : JsonSchema() {
    override fun toString() = "MapSchema($valueSchema)"
}
