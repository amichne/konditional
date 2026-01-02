package io.amichne.konditional.serialization

import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.kontracts.schema.ObjectSchema
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance

/**
 * Extracts an ObjectSchema from a KClass that implements KotlinEncodeable.
 *
 * Attempts multiple strategies to obtain the schema:
 * 1. Object instance (for singleton objects)
 * 2. No-arg constructor (for data classes with defaults)
 * 3. Companion object property (if schema is defined statically)
 *
 * @param kClass The class to extract schema from
 * @return ObjectSchema if found, null otherwise
 */
internal fun extractSchema(kClass: KClass<*>): ObjectSchema? {
    // Strategy 1: Object instance (singletons)
    kClass.objectInstance?.let { instance ->
        return (instance as? KotlinEncodeable<*>)?.schema as? ObjectSchema
    }

    // Strategy 2: Create instance via no-arg constructor
    try {
        val instance = kClass.createInstance()
        return (instance as? KotlinEncodeable<*>)?.schema as? ObjectSchema
    } catch (e: Exception) {
        // No no-arg constructor or failed to create - try companion object
    }

    // Strategy 3: Companion object with schema property
    kClass.companionObject?.let { companionClass ->
        val companionInstance = kClass.companionObjectInstance
        companionClass.members
            .find { it.name == "schema" }
            ?.call(companionInstance)
            ?.let { return it as? ObjectSchema }
    }

    return null
}
