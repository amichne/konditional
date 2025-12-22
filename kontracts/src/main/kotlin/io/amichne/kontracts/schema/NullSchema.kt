package io.amichne.kontracts.schema

/**
 * Schema for null values.
 */
data class NullSchema(
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = true,
    override val example: Any? = null,
    override val deprecated: Boolean = false
) : JsonSchema<Any>() {
    override fun toString() = "NullSchema"
}
