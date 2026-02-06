package io.amichne.kontracts.schema

import kotlin.reflect.KProperty1

/**
 * Property-backed schema definition for a single field on [T].
 *
 * This retains the reflective Kotlin property in the model itself instead of only storing the
 * property name as a string key.
 */
data class ReflectiveFieldSchema<T : Any, out V>(
    val property: KProperty1<T, V>,
    val schema: JsonSchema<*>,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val description: String? = null,
    val deprecated: Boolean = false,
) {
    val name: String = property.name

    fun toFieldSchema(): FieldSchema =
        FieldSchema(
            schema = schema,
            required = required,
            defaultValue = defaultValue,
            description = description,
            deprecated = deprecated,
        )
}
