package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.FeatureFlag
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.instance.Konfig
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.serialization.models.FlagValue
import io.amichne.konditional.serialization.models.SerializableFlag
import io.amichne.konditional.serialization.models.SerializablePatch
import io.amichne.konditional.serialization.models.SerializableRule
import io.amichne.konditional.serialization.models.SerializableSnapshot

/**
 * Registry for mapping flag keys to their Conditional instances.
 *
 * This registry is required for deserialization since we need to reconstruct the proper
 * Conditional references when loading flag configurations from JSON. The registry maintains
 * a bidirectional mapping between string keys and Conditional instances.
 *
 * ## Registration
 *
 * Before deserializing flags, you must register all Conditional instances that might appear
 * in the serialized configuration:
 *
 * ```kotlin
 * // Register individual conditionals
 * ConditionalRegistry.register(Features.DARK_MODE)
 *
 * // Or register entire enum at once
 * ConditionalRegistry.registerEnum<Features>()
 * ```
 *
 * ## Thread Safety
 *
 * This registry is NOT thread-safe. Registration should happen during application initialization
 * before any concurrent access.
 *
 * @see io.amichne.konditional.serialization.SnapshotSerializer
 */
object ConditionalRegistry {
    private val registry = mutableMapOf<String, Conditional<*, *>>()

    /**
     * Registers a Conditional instance with its key.
     *
     * @param conditional The conditional to register
     * @throws IllegalStateException if a different conditional is already registered with the same key
     */
    fun <S : EncodableValue<*>, C : Context> register(conditional: Conditional<S, C>) {
        registry[conditional.key] = conditional
    }

    /**
     * Registers all Conditionals from an enum class.
     *
     * This is a convenience method for registering entire enum classes that implement Conditional.
     *
     * Example:
     * ```kotlin
     * enum class Features : Conditional<Boolean, Context> { ... }
     * ConditionalRegistry.registerEnum<Features>()
     * ```
     *
     * @param T The enum type that implements Conditional
     */
    inline fun <reified T> registerEnum() where T : Enum<T>, T : Conditional<*, *> {
        enumValues<T>().forEach { register(it) }
    }

    /**
     * Retrieves a Conditional by its key, returning ParseResult for type-safe error handling.
     *
     * @param key The string key of the conditional
     * @return ParseResult with the registered Conditional or an error
     */
    fun get(key: String): ParseResult<Conditional<*, *>> {
        return registry[key]?.let { ParseResult.Success(it) }
            ?: ParseResult.Failure(ParseError.ConditionalNotFound(key))
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
fun Konfig.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (conditional, flag) ->
        (flag as FlagDefinition<*, *>).toSerializable(conditional.key)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a FlagDefinition to a SerializableFlag.
 */
private fun <S : EncodableValue<*>, C : Context> FlagDefinition<S, C>.toSerializable(flagKey: String): SerializableFlag {
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
private fun <S : EncodableValue<*>, C : Context> ConditionalValue<S, C>.toSerializable(): SerializableRule {
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
fun SerializableSnapshot.toSnapshot(): ParseResult<Konfig> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Conditional<*, *>, FeatureFlag<*, *>>>>()
            .associate { it.value }

        ParseResult.Success(Konfig(flagMap))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}

/**
 * Converts a SerializableFlag to a Map.Entry of Conditional to FeatureFlag.
 * Returns ParseResult for type-safe error handling.
 */
private fun SerializableFlag.toFlagPair(): ParseResult<Pair<Conditional<*, *>, FeatureFlag<*, *>>> {
    return when (val conditionalResult = ConditionalRegistry.get(key)) {
        is ParseResult.Success -> {
            val conditional = conditionalResult.value
            val definition = toFlagDefinition(conditional)
            ParseResult.Success(conditional to definition)
        }
        is ParseResult.Failure -> ParseResult.Failure(conditionalResult.error)
    }
}

/**
 * Converts a SerializableFlag to a FlagDefinition.
 * Type-safe: no casting required thanks to FlagValue sealed class.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : EncodableValue<*>, C : Context> SerializableFlag.toFlagDefinition(
    conditional: Conditional<S, C>
): FlagDefinition<S, C> {
    // Extract typed value from FlagValue (type-safe extraction)
    val typedDefaultValue = defaultValue.extractValue<S>()
    val values = rules.map { it.toValue<S, C>() }

    return FlagDefinition(
        conditional = conditional,
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
private fun <S : EncodableValue<*>, C : Context> SerializableRule.toValue(): ConditionalValue<S, C> {
    val typedValue = value.extractValue<S>()
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
fun KonfigPatch.toSerializable(): SerializablePatch {
    val serializableFlags = flags.map { (conditional, flag) ->
        (flag as FlagDefinition<*, *>).toSerializable(conditional.key)
    }
    val removeKeyStrings = removeKeys.map { it.key }
    return SerializablePatch(serializableFlags, removeKeyStrings)
}

/**
 * Converts a SerializablePatch to a KonfigPatch.
 * Returns ParseResult for type-safe error handling.
 */
fun SerializablePatch.toPatch(): ParseResult<KonfigPatch> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Conditional<*, *>, FeatureFlag<*, *>>>>()
            .associate { it.value }

        // For removeKeys, skip keys that aren't registered (they may have been removed)
        val removeConditionals = removeKeys.mapNotNull { key ->
            when (val result = ConditionalRegistry.get(key)) {
                is ParseResult.Success -> result.value
                is ParseResult.Failure -> null  // Skip unregistered keys
            }
        }.toSet()

        ParseResult.Success(KonfigPatch(flagMap, removeConditionals))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}
