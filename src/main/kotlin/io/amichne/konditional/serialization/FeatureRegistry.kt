package io.amichne.konditional.serialization

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.EncodableValue

/**
 * Registry for mapping flag keys to their Feature instances.
 *
 * This registry is required for deserialization since we need to reconstruct the proper
 * Feature references when loading flag configurations from JSON. The registry maintains
 * a bidirectional mapping between string keys and Feature instances.
 *
 * ## Registration
 *
 * Before deserializing flags, you must register all Feature instances that might appear
 * in the serialized configuration:
 *
 * ```kotlin
 * // Register individual conditionals
 * FeatureRegistry.register(Features.DARK_MODE)
 *
 * // Or register entire enum at once
 * FeatureRegistry.registerEnum<Features>()
 * ```
 *
 * ## Thread Safety
 *
 * This registry is NOT thread-safe. Registration should happen during application initialization
 * before any concurrent access.
 *
 * @see SnapshotSerializer
 */
object FeatureRegistry {
    private val registry = mutableMapOf<String, Feature<*, *, *, *>>()

    /**
     * Registers a Feature instance with its key.
     *
     * @param feature The feature to register
     * @throws IllegalStateException if a different conditional is already registered with the same key
     */
    fun <S : EncodableValue<T>, T : Any, C : Context> register(feature: Feature<S, T, C, *>) {
        registry[feature.id] = feature
    }

    /**
     * Retrieves a Feature by its key, returning ParseResult for type-safe error handling.
     *
     * Internal: Used by serialization infrastructure for deserialization.
     *
     * @param key The string key of the conditional
     * @return ParseResult with the registered Feature or an error
     */
    internal fun get(key: String): ParseResult<Feature<*, *, *, *>> {
        return registry[key]?.let { ParseResult.Success(it) }
               ?: ParseResult.Failure(ParseError.FeatureNotFound(key))
    }

    /**
     * Checks if a key is registered.
     *
     * Internal: Used for validation during deserialization.
     *
     * @param key The string key to check
     * @return true if the key is registered, false otherwise
     */
    internal fun contains(key: String): Boolean = registry.containsKey(key)

    /**
     * Clears all registrations.
     *
     * Internal: For testing to ensure a clean state between tests.
     * Should not be called in production code.
     */
    @org.jetbrains.annotations.TestOnly
    internal fun clear() {
        registry.clear()
    }
}
