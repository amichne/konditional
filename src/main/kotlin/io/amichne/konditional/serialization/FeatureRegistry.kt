package io.amichne.konditional.serialization

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.values.Identifier
import java.util.concurrent.ConcurrentHashMap

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
 * The underlying storage is thread-safe and supports concurrent registration and reads.
 * The only semantic requirement is ordering: callers must ensure that any feature they expect
 * to deserialize has been registered before deserialization begins.
 *
 * @see SnapshotSerializer
 */
internal object FeatureRegistry {
    private val registry = ConcurrentHashMap<Identifier, Feature<*, *, *>>()

    /**
     * Registers a Feature instance with its key.
     *
     * @param feature The feature to register
     * @throws IllegalStateException if a different feature is already registered with the same key
     */
    fun <T : Any, C : Context> register(feature: Feature<T, C, *>) {
        val existing = registry.putIfAbsent(feature.id, feature)
        check(existing == null || existing === feature) {
            "Feature already registered for id='${feature.id}' (key='${feature.key}'): existing=$existing, attempted=$feature"
        }
    }

    /**
     * Retrieves a Feature by its key, returning ParseResult for type-safe error handling.
     *
     * Internal: Used by serialization infrastructure for deserialization.
     *
     * @param key The string key of the conditional
     * @return ParseResult with the registered Feature or an error
     */
    internal fun get(key: Identifier): ParseResult<Feature<*, *, *>> =
        registry[key]?.let { ParseResult.Success(it) }
            ?: ParseResult.Failure(ParseError.FeatureNotFound(key))

    /**
     * Checks if a key is registered.
     *
     * Internal: Used for validation during deserialization.
     *
     * @param key The string key to check
     * @return true if the key is registered, false otherwise
     */
    internal fun contains(key: Identifier): Boolean = registry.containsKey(key)

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
