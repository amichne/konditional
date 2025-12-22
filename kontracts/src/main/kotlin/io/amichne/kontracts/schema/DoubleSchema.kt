package io.amichne.kontracts.schema

/**
 * Schema for double/decimal values.
 * Supports OpenAPI numeric constraints.
 */
data class DoubleSchema(
    override val title: String? = null,
    override val description: String? = null,
    override val default: Double? = null,
    override val nullable: Boolean = false,
    override val example: Double? = null,
    override val deprecated: Boolean = false,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val enum: List<Double>? = null,
    val format: String? = null
) : JsonSchema<Double>() {
    override fun toString() = "DoubleSchema"
}
