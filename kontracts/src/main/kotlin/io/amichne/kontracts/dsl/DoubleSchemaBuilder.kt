package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.DoubleSchema

@JsonSchemaBuilderDsl
open class DoubleSchemaBuilder : JsonSchemaBuilder {
    var title: String? = null
    var description: String? = null
    var default: Any? = null
    var nullable: Boolean = false
    var example: Any? = null
    var deprecated: Boolean = false
    var minimum: Double? = null
    var maximum: Double? = null
    var enum: List<Double>? = null
    var format: String? = null
    fun build() = DoubleSchema(
        title,
        description,
        default,
        nullable,
        example,
        deprecated,
        minimum,
        maximum,
        enum,
        format
    )
}
