package io.amichne.kontracts.schema

/**
 * Schema for homogeneous arrays.
 * @param elementSchema The schema for all elements in the array
 */
data class ArraySchema(
    val elementSchema: JsonSchema,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean = false
) : JsonSchema() {
    override fun toString() = "ArraySchema($elementSchema)"
}
