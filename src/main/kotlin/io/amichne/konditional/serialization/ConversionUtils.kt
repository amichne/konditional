package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.Feature
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.FlagDefinitionImpl
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule

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
 * @see io.amichne.konditional.serialization.SnapshotSerializer
 */
object FeatureRegistry {
    private val registry = mutableMapOf<String, Feature<*, *, *>>()

    /**
     * Registers a Feature instance with its key.
     *
     * @param conditional The conditional to register
     * @throws IllegalStateException if a different conditional is already registered with the same key
     */
    fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> register(conditional: Feature<S, T, C>) {
        registry[conditional.key] = conditional
    }

    /**
     * Registers all Features from an enum class.
     *
     * This is a convenience method for registering entire enum classes that implement Feature.
     *
     * Example:
     * ```kotlin
     * enum class Features : Feature<Boolean, Context> { ... }
     * FeatureRegistry.registerEnum<Features>()
     * ```
     *
     * @param T The enum type that implements Feature
     */
    inline fun <reified T> registerEnum() where T : Enum<T>, T : Feature<*, *, *> {
        enumValues<T>().forEach { register(it) }
    }

    /**
     * Retrieves a Feature by its key, returning ParseResult for type-safe error handling.
     *
     * @param key The string key of the conditional
     * @return ParseResult with the registered Feature or an error
     */
    fun get(key: String): ParseResult<Feature<*, *, *>> {
        return registry[key]?.let { ParseResult.Success(it) }
            ?: ParseResult.Failure(ParseError.FeatureNotFound(key))
    }

    /**
     * Checks if a key is registered.
     *
     * @param key The string key to check
     * @return true if the key is registered, false otherwise
     */
    fun contains(key: String): Boolean = registry.containsKey(key)

    /**
     * Clears all registrations.
     *
     * This is primarily useful for testing to ensure a clean state between tests.
     * Should not be called in production code.
     */
    fun clear() {
        registry.clear()
    }
}

/**
 * Converts a Konfig to a SerializableSnapshot.
 */
internal fun Konfig.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (conditional, flag) ->
        (flag as FlagDefinitionImpl<*, *, *>).toSerializable(conditional.key)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a FlagDefinitionImpl to a SerializableFlag.
 */
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> FlagDefinitionImpl<S, T, C>.toSerializable(flagKey: String): SerializableFlag {
    return SerializableFlag(
        key = flagKey,
        defaultValue = FlagValue.from(defaultValue),
        salt = salt,
        isActive = isActive,
        rules = values.map { it.toSerializable() }
    )
}

/**
 * Converts a ConditionalValue to a SerializableRule.
 */
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> ConditionalValue<S, T, C>.toSerializable(): SerializableRule {
    return SerializableRule(
        value = FlagValue.from(value),
        rampUp = rule.rollout.value,
        note = rule.note,
        locales = rule.baseEvaluable.locales.map { it.name }.toSet(),
        platforms = rule.baseEvaluable.platforms.map { it.name }.toSet(),
        versionRange = rule.baseEvaluable.versionRange
    )
}

/**
 * Converts a SerializableSnapshot to a Konfig.
 * Returns ParseResult for type-safe error handling.
 */
internal fun SerializableSnapshot.toSnapshot(): ParseResult<Konfig> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>>>()
            .associate { it.value }

        ParseResult.Success(Konfig(flagMap))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}

/**
 * Converts a SerializableFlag to a Map.Entry of Feature to FlagDefinition.
 * Returns ParseResult for type-safe error handling.
 */
private fun SerializableFlag.toFlagPair(): ParseResult<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>> {
    return when (val conditionalResult = FeatureRegistry.get(key)) {
        is ParseResult.Success -> {
            val conditional = conditionalResult.value
            val definition = toFlagDefinition(conditional)
            ParseResult.Success(conditional to definition)
        }
        is ParseResult.Failure -> ParseResult.Failure(conditionalResult.error)
    }
}

/**
 * Converts a SerializableFlag to a FlagDefinitionImpl.
 * Type-safe: no casting required thanks to FlagValue sealed class.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> SerializableFlag.toFlagDefinition(
    conditional: Feature<S, T, C>
): FlagDefinitionImpl<S, T, C> {
    // Extract typed value from FlagValue (type-safe extraction)
    val typedDefaultValue = defaultValue.extractValue<T>()
    val values = rules.map { it.toValue<S, T, C>() }

    return FlagDefinitionImpl(
        feature = conditional,
        values = values,
        defaultValue = typedDefaultValue,
        salt = salt,
        isActive = isActive
    )
}

/**
 * Extracts the value from a FlagValue with type safety.
 * The unchecked cast is safe because FlagValue guarantees type correspondence.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Any> FlagValue<*>.extractValue(): T = this.value as T

/**
 * Converts a SerializableRule to a ConditionalValue.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> SerializableRule.toValue(): ConditionalValue<S, T, C> {
    val typedValue = value.extractValue<T>()
    val rule = toRule<C>()
    return rule.targetedBy(typedValue)
}

/**
 * Converts a SerializableRule to a Rule.
 * Simplified: VersionRange is already the domain type.
 */
private fun <C : Context> SerializableRule.toRule(): Rule<C> {
    return Rule(
        rollout = Rollout.of(rampUp),
        note = note,
        locales = locales.map { AppLocale.valueOf(it) }.toSet(),
        platforms = platforms.map { Platform.valueOf(it) }.toSet(),
        versionRange = versionRange ?: io.amichne.konditional.rules.versions.Unbounded
    )
}

/**
 * Converts a KonfigPatch to a SerializablePatch.
 */
internal fun KonfigPatch.toSerializable(): SerializablePatch {
    val serializableFlags = flags.map { (conditional, flag) ->
        (flag as FlagDefinitionImpl<*, *, *>).toSerializable(conditional.key)
    }
    val removeKeyStrings = removeKeys.map { it.key }
    return SerializablePatch(serializableFlags, removeKeyStrings)
}

/**
 * Converts a SerializablePatch to a KonfigPatch.
 * Returns ParseResult for type-safe error handling.
 */
internal fun SerializablePatch.toPatch(): ParseResult<KonfigPatch> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>>>()
            .associate { it.value }

        // For removeKeys, skip keys that aren't registered (they may have been removed)
        val removeFeatures = removeKeys.mapNotNull { key ->
            when (val result = FeatureRegistry.get(key)) {
                is ParseResult.Success -> result.value
                is ParseResult.Failure -> null  // Skip unregistered keys
            }
        }.toSet()

        ParseResult.Success(KonfigPatch(flagMap, removeFeatures))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}
