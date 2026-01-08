@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.kontracts.schema.JsonSchema
import io.amichne.kontracts.schema.ObjectSchema
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance

/**
 * Extracts an ObjectSchema from a KClass that implements Konstrained.
 *
 * Attempts multiple strategies to obtain the schema:
 * 1. Object instance (for singleton objects)
 * 2. No-arg constructor (for data classes with defaults)
 * 3. Companion object property (if schema is defined statically)
 *
 * @param kClass The class to extract schema from
 * @return ObjectSchema if found, null otherwise
 */
@KonditionalInternalApi
fun extractSchema(kClass: KClass<*>): ObjectSchema? {
    val schemaFromObject =
        (kClass.objectInstance as? Konstrained<*>)?.schema.asObjectSchemaOrNull()

    val schemaFromConstructor =
        runCatching { (kClass.createInstance() as? Konstrained<*>)?.schema }
            .getOrNull()
            .asObjectSchemaOrNull()

    val schemaFromCompanion =
        kClass.companionObjectInstance
            ?.let { instance ->
                kClass.companionObject
                    ?.members
                    ?.firstOrNull { it.name == "schema" }
                    ?.call(instance)
            }.asObjectSchemaOrNull()

    return schemaFromObject ?: schemaFromConstructor ?: schemaFromCompanion
}

private fun Any?.asObjectSchemaOrNull(): ObjectSchema? =
    (this as? JsonSchema<*>)?.let { schema ->
        runCatching { schema.asObjectSchema() }.getOrNull()
    }
