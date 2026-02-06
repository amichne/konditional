package io.amichne.kontracts.schema

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Object schema backed by Kotlin property references.
 *
 * Unlike plain [ObjectSchema], this model stores [KProperty1] entries so callers can
 * query schema information using compile-time checked property references.
 */
data class ReflectiveObjectSchema<T : Any>(
    val owner: KClass<T>,
    val properties: List<ReflectiveFieldSchema<T, *>>,
    override val title: String? = null,
    override val description: String? = null,
    override val default: Map<String, Any?>? = null,
    override val nullable: Boolean = false,
    override val example: Map<String, Any?>? = null,
    override val deprecated: Boolean = false,
    override val required: Set<String>? = null,
) : JsonSchema<Map<String, Any?>>(), ObjectTraits {
    override val type: OpenApi.Type = OpenApi.Type.OBJECT

    override val fields: Map<String, FieldSchema> =
        properties.associate { propertySchema ->
            propertySchema.name to propertySchema.toFieldSchema()
        }

    private val propertiesByName: Map<String, ReflectiveFieldSchema<T, *>> =
        properties.associateBy { propertySchema -> propertySchema.name }

    @Suppress("UNCHECKED_CAST")
    fun <V> field(property: KProperty1<T, V>): ReflectiveFieldSchema<T, V>? =
        propertiesByName[property.name] as? ReflectiveFieldSchema<T, V>

    fun toObjectSchema(): ObjectSchema =
        ObjectSchema(
            fields = fields,
            title = title,
            description = description,
            default = default,
            nullable = nullable,
            example = example,
            deprecated = deprecated,
            required = required,
        )

    override fun toString(): String =
        "ReflectiveObjectSchema(owner=${owner.simpleName}, properties=${properties.map { it.name }})"
}
