package io.amichne.kontracts.schema

/**
 * Schema reference pointing to a component schema path.
 */
data class RefSchema(
    val ref: String,
) : JsonSchema<Any>() {
    override val type: OpenApi.Type = OpenApi.Type.OBJECT
}
