package io.amichne.kontracts.schema

/**
 * Schema for integer values.
 * Supports OpenAPI numeric constraints.
 */
data class IntSchema(
    override val title: String? = null,
    override val description: String? = null,
    override val default: Int? = null,
    override val nullable: Boolean = false,
    override val example: Int? = null,
    override val deprecated: Boolean = false,
    val minimum: Int? = null,
    val maximum: Int? = null,
    val enum: List<Int>? = null
) : JsonSchema<Int>() {
    override fun toString() = "IntSchema"
}
