package io.amichne.kontracts.schema

/**
 * Schema representing an all-of composition of multiple schemas.
 */
data class AllOfSchema(
    val options: List<JsonSchema<*>>,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Any? = null,
    override val nullable: Boolean = false,
    override val example: Any? = null,
    override val deprecated: Boolean = false
) : JsonSchema<Any>() {
    override val type: OpenApi.Type
        get() = options.firstOrNull()?.type ?: OpenApi.Type.OBJECT
    override fun toString() = "AllOfSchema(options=${options.size})"
}
