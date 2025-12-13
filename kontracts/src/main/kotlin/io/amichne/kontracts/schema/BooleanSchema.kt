package io.amichne.kontracts.schema

/**
 * Schema for boolean values.
 */
data class BooleanSchema(
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false
) : JsonSchema() {
    override fun toString() = "BooleanSchema"
}
