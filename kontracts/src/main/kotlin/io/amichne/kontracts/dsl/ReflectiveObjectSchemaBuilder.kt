package io.amichne.kontracts.dsl

import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ReflectiveFieldSchema
import io.amichne.kontracts.schema.ReflectiveObjectSchema
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@JsonSchemaBuilderDsl
class ReflectiveObjectSchemaBuilder<T : Any>(
    private val owner: KClass<T>,
) {
    var title: String? = null
    var description: String? = null
    var default: Map<String, Any?>? = null
    var nullable: Boolean = false
    var example: Map<String, Any?>? = null
    var deprecated: Boolean = false
    var required: Set<String>? = null

    private val properties = mutableListOf<ReflectiveFieldSchema<T, *>>()

    fun <V> required(
        property: KProperty1<T, V>,
        schema: JsonSchema<*>,
        description: String? = null,
        defaultValue: Any? = null,
        deprecated: Boolean = false,
    ) {
        registerField(
            ReflectiveFieldSchema(
                property = property,
                schema = schema,
                required = true,
                defaultValue = defaultValue ?: schema.default,
                description = description ?: schema.description,
                deprecated = deprecated || schema.deprecated,
            ),
        )
    }

    fun <V> optional(
        property: KProperty1<T, V>,
        schema: JsonSchema<*>,
        description: String? = null,
        defaultValue: Any? = null,
        deprecated: Boolean = false,
    ) {
        registerField(
            ReflectiveFieldSchema(
                property = property,
                schema = schema,
                required = false,
                defaultValue = defaultValue ?: schema.default,
                description = description ?: schema.description,
                deprecated = deprecated || schema.deprecated,
            ),
        )
    }

    fun build(): ReflectiveObjectSchema<T> =
        ReflectiveObjectSchema(
            owner = owner,
            properties = properties.toList(),
            title = title,
            description = description,
            default = default,
            nullable = nullable,
            example = example,
            deprecated = deprecated,
            required = required,
        )

    private fun registerField(field: ReflectiveFieldSchema<T, *>) {
        check(properties.none { existing -> existing.name == field.name }) {
            "Schema for ${owner.qualifiedName} already defines property '${field.name}'"
        }
        properties += field
    }
}

inline fun <reified T : Any> reflectiveSchema(
    builder: ReflectiveObjectSchemaBuilder<T>.() -> Unit,
): ReflectiveObjectSchema<T> = ReflectiveObjectSchemaBuilder(T::class).apply(builder).build()
