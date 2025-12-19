package io.amichne.kontracts.schema

/**
 * Schema representing a one-of union of multiple schemas.
 */
data class OneOfSchema(
    val options: List<JsonSchema>,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false
) : JsonSchema() {
    override fun toString() = "OneOfSchema(options=${options.size})"
}
