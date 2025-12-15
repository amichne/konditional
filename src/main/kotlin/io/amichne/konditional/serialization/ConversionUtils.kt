package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationPatch
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.EncodableValue
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableFlag
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.internal.serialization.models.SerializableRule
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.values.Identifier
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

/**
 * Converts a Configuration to a SerializableSnapshot.
 */
internal fun Configuration.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (conditional, flag) ->
        flag.toSerializable(conditional.id)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a FlagDefinition to a SerializableFlag.
 */
private fun <S : EncodableValue<T>, T : Any, C : Context> FlagDefinition<S, T, C, *>.toSerializable(
    flagKey: Identifier,
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
private fun <S : EncodableValue<T>, T : Any, C : Context> ConditionalValue<S, T, C, *>.toSerializable(): SerializableRule {
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
private fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> SerializableFlag.toFlagDefinition(
    conditional: Feature<S, T, C, M>,
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
 *
 * This performs boundary decoding for types that are serialized in a tagged representation:
 * - Enums: stored as (enumClassName, enumConstantName)
 * - KotlinEncodeable/data classes: stored as (dataClassName, primitive field map)
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Any> FlagValue<*>.extractValue(): T =
    when (this) {
        is FlagValue.EnumValue -> decodeEnum(enumClassName, value) as T
        is FlagValue.DataClassValue -> decodeDataClass(dataClassName, value) as T
        else -> value as T
    }

private fun decodeEnum(
    enumClassName: String,
    enumConstantName: String,
): Enum<*> {
    val enumClass = runCatching { Class.forName(enumClassName).asSubclass(Enum::class.java) }
        .getOrElse { throw IllegalArgumentException("Failed to load enum class '$enumClassName': ${it.message}") }

    @Suppress("UNCHECKED_CAST")
    return java.lang.Enum.valueOf(enumClass as Class<out Enum<*>>, enumConstantName)
}

private fun decodeDataClass(
    dataClassName: String,
    fields: Map<String, Any?>,
): Any {
    val kClass = runCatching { Class.forName(dataClassName).kotlin }
        .getOrElse { throw IllegalArgumentException("Failed to load data class '$dataClassName': ${it.message}") }

    val constructor = kClass.primaryConstructor
                      ?: throw IllegalArgumentException("Custom type '${kClass.simpleName}' must have a primary constructor")

    val args = constructor.parameters
        .associateWith { param -> resolveConstructorArg(fields, param) }
        .filterValues { it != Unset }
        .mapValues { (_, v) -> if (v === NullValue) null else v }

    return constructor.callBy(args)
}

private object Unset
private object NullValue

private fun resolveConstructorArg(
    fields: Map<String, Any?>,
    param: KParameter,
): Any {
    val name = param.name ?: throw IllegalArgumentException("Constructor parameter has no name")

    if (!fields.containsKey(name)) {
        return if (param.isOptional) Unset else throw IllegalArgumentException("Required field '$name' is missing")
    }

    val raw = fields[name]
    if (raw == null) return NullValue

    val target = param.type.classifier as? KClass<*>
    return coerceValue(raw, target)
}

private fun coerceValue(
    value: Any,
    target: KClass<*>?,
): Any = when {
    target == null -> value
    target == String::class -> value.toString()
    target == Boolean::class -> when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: error("Cannot coerce '$value' to Boolean")
        else -> error("Cannot coerce ${value::class.simpleName} to Boolean")
    }
    target == Int::class -> when (value) {
        is Int -> value
        is Double -> value.toInt()
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Cannot coerce ${value::class.simpleName} to Int")
    }
    target == Double::class -> when (value) {
        is Double -> value
        is Int -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Cannot coerce ${value::class.simpleName} to Double")
    }
    target.java.isEnum -> {
        val enumConstantName = value as? String ?: error("Enum values must be strings, got ${value::class.simpleName}")
        decodeEnum(target.java.name, enumConstantName)
    }
    value is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        decodeDataClass(target.java.name, value as Map<String, Any?>)
    }
    else -> value
}

/**
 * Converts a SerializableRule to a ConditionalValue.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace> SerializableRule.toValue(): ConditionalValue<S, T, C, M> {
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
        rollout = Rampup.of(rampUp),
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
        flag.toSerializable(conditional.id)
    }
    val removeKeyStrings = removeKeys.map { it.id }
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
