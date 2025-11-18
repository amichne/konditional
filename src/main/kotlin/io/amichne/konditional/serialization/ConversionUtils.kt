package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationPatch
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
import io.amichne.konditional.rules.versions.Unbounded

/**
 * Converts a Configuration to a SerializableSnapshot.
 */
internal fun Configuration.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (conditional, flag) ->
        flag.toSerializable(conditional.key)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a FlagDefinition to a SerializableFlag.
 */
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> FlagDefinition<S, T, C, *>.toSerializable(
    flagKey: String
): SerializableFlag {
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
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context> ConditionalValue<S, T, C, *>.toSerializable(): SerializableRule {
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
 * Converts a SerializableSnapshot to a Configuration.
 * Returns ParseResult for type-safe error handling.
 */
internal fun SerializableSnapshot.toSnapshot(): ParseResult<Configuration> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>>>()
            .associate { it.value }

        ParseResult.Success(Configuration(flagMap))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}

/**
 * Converts a SerializableFlag to a Map.Entry of Feature to FlagDefinition.
 * Returns ParseResult for type-safe error handling.
 */
private fun SerializableFlag.toFlagPair(): ParseResult<Pair<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>> {
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
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : Namespace> SerializableFlag.toFlagDefinition(
    conditional: Feature<S, T, C, M>
): FlagDefinition<S, T, C, M> {
    // Extract typed value from FlagValue (type-safe extraction)
    val typedDefaultValue = defaultValue.extractValue<T>()
    val values = rules.map { it.toValue<S, T, C, M>() }

    return FlagDefinition(
        feature = conditional,
        bounds = values,
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
private fun <S : io.amichne.konditional.core.types.EncodableValue<T>, T : Any, C : Context, M : Namespace> SerializableRule.toValue(): ConditionalValue<S, T, C, M> {
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
        versionRange = (versionRange ?: Unbounded())
    )
}

/**
 * Converts a ConfigurationPatch to a SerializablePatch.
 */
internal fun ConfigurationPatch.toSerializable(): SerializablePatch {
    val serializableFlags = flags.map { (conditional, flag) ->
        flag.toSerializable(conditional.key)
    }
    val removeKeyStrings = removeKeys.map { it.key }
    return SerializablePatch(serializableFlags, removeKeyStrings)
}

/**
 * Converts a SerializablePatch to a ConfigurationPatch.
 * Returns ParseResult for type-safe error handling.
 */
internal fun SerializablePatch.toPatch(): ParseResult<ConfigurationPatch> {
    return try {
        val flagResults = flags.map { it.toFlagPair() }

        // Check for any failures
        val failures = flagResults.filterIsInstance<ParseResult.Failure>()
        if (failures.isNotEmpty()) {
            return ParseResult.Failure(failures.first().error)
        }

        // Extract successful values
        val flagMap = flagResults
            .filterIsInstance<ParseResult.Success<Pair<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>>>()
            .associate { it.value }

        // For removeKeys, skip keys that aren't registered (they may have been removed)
        val removeFeatures = removeKeys.mapNotNull { key ->
            when (val result = FeatureRegistry.get(key)) {
                is ParseResult.Success -> result.value
                is ParseResult.Failure -> null  // Skip unregistered keys
            }
        }.toSet()

        ParseResult.Success(ConfigurationPatch(flagMap, removeFeatures))
    } catch (e: Exception) {
        ParseResult.Failure(ParseError.InvalidSnapshot(e.message ?: "Unknown error"))
    }
}
