package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.Flags
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.TargetedValue
import io.amichne.konditional.rules.TargetedValue.Companion.targetedBy
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.serialization.models.SerializableFlag
import io.amichne.konditional.serialization.models.SerializableRule
import io.amichne.konditional.serialization.models.SerializableSnapshot
import io.amichne.konditional.serialization.models.SerializableVersion
import io.amichne.konditional.serialization.models.SerializableVersionRange
import io.amichne.konditional.core.ValueType
import io.amichne.konditional.serialization.models.VersionRangeType

/**
 * Registry for mapping flag keys to their Conditional instances.
 * This is required for deserialization since we need to reconstruct the proper Conditional references.
 */
object ConditionalRegistry {
    private val registry = mutableMapOf<String, Conditional<*, *>>()

    /**
     * Registers a Conditional instance with its key.
     */
    fun <S : Any, C : Context> register(conditional: Conditional<S, C>) {
        registry[conditional.key] = conditional
    }

    /**
     * Registers all Conditionals from an enum class.
     */
    inline fun <reified T> registerEnum() where T : Enum<T>, T : Conditional<*, *> {
        enumValues<T>().forEach { register(it) }
    }

    /**
     * Retrieves a Conditional by its key.
     * @throws IllegalArgumentException if the key is not registered
     */
    @Suppress("UNCHECKED_CAST")
    fun <S : Any, C : Context> get(key: String): Conditional<S, C> {
        return registry[key] as? Conditional<S, C>
            ?: throw IllegalArgumentException("Conditional with key '$key' not found in registry. Did you forget to register it?")
    }

    /**
     * Checks if a key is registered.
     */
    fun contains(key: String): Boolean = registry.containsKey(key)

    /**
     * Clears all registrations (useful for testing).
     */
    fun clear() {
        registry.clear()
    }
}

/**
 * Converts a Flags.Snapshot to a SerializableSnapshot.
 */
fun Flags.Snapshot.toSerializable(): SerializableSnapshot {
    val serializableFlags = flags.map { (conditional, flagEntry) ->
        flagEntry.definition.toSerializable(conditional.key)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a FlagDefinition to a SerializableFlag.
 */
private fun <S : Any, C : Context> FlagDefinition<S, C>.toSerializable(flagKey: String): SerializableFlag {
    return SerializableFlag(
        key = flagKey,
        type = defaultValue.toValueType(),
        defaultValue = defaultValue,
        salt = salt,
        isActive = isActive,
        rules = bounds.map { it.toSerializable() }
    )
}

/**
 * Converts a TargetedValue to a SerializableRule.
 */
private fun <S : Any, C : Context> TargetedValue<S, C>.toSerializable(): SerializableRule {
    return SerializableRule(
        value = SerializableRule.SerializableValue(
            value = value,
            type = value.toValueType()
        ),
        rampUp = rule.rollout.value,
        note = rule.note,
        locales = rule.locales.map { it.name }.toSet(),
        platforms = rule.platforms.map { it.name }.toSet(),
        versionRange = rule.versionRange.toSerializableVersionRange()
    )
}

/**
 * Converts a VersionRange to a SerializableVersionRange.
 */
private fun VersionRange.toSerializableVersionRange(): SerializableVersionRange? {
    return when (this) {
        is Unbounded -> SerializableVersionRange(VersionRangeType.UNBOUNDED)
        is LeftBound -> SerializableVersionRange(
            type = VersionRangeType.MIN_BOUND,
            min = min.toSerializableVersion()
        )
        is RightBound -> SerializableVersionRange(
            type = VersionRangeType.MAX_BOUND,
            max = max.toSerializableVersion()
        )
        is FullyBound -> SerializableVersionRange(
            type = VersionRangeType.MIN_AND_MAX_BOUND,
            min = min.toSerializableVersion(),
            max = max.toSerializableVersion()
        )
    }
}

/**
 * Converts a Version to a SerializableVersion.
 */
private fun Version.toSerializableVersion(): SerializableVersion {
    return SerializableVersion(major, minor, patch)
}

/**
 * Infers the ValueType from an Any value.
 */
private fun Any.toValueType(): ValueType {
    return when (this) {
        is Boolean -> ValueType.BOOLEAN
        is String -> ValueType.STRING
        is Int -> ValueType.INT
        is Long -> ValueType.LONG
        is Double -> ValueType.DOUBLE
        else -> throw IllegalArgumentException("Unsupported value type: ${this::class.simpleName}")
    }
}

/**
 * Converts a SerializableSnapshot to a Flags.Snapshot.
 * @throws IllegalArgumentException if any flag keys are not registered in ConditionalRegistry
 */
fun SerializableSnapshot.toSnapshot(): Flags.Snapshot {
    val flagMap = flags.associate { serializableFlag ->
        serializableFlag.toFlagEntry()
    }
    return Flags.Snapshot(flagMap)
}

/**
 * Converts a SerializableFlag to a Map.Entry of Conditional to FlagEntry.
 */
@Suppress("UNCHECKED_CAST")
private fun SerializableFlag.toFlagEntry(): Pair<Conditional<*, *>, Flags.FlagEntry<*, *>> {
    val conditional = ConditionalRegistry.get<Any, Context>(key)
    val definition = toFlagDefinition(conditional as Conditional<Any, Context>)
    val flagEntry = Flags.FlagEntry(definition)
    return conditional to flagEntry
}

/**
 * Converts a SerializableFlag to a FlagDefinition.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : Any, C : Context> SerializableFlag.toFlagDefinition(
    conditional: Conditional<S, C>
): FlagDefinition<S, C> {
    val typedDefaultValue = defaultValue.castToType(type) as S
    val typedBounds = rules.map { it.toTargetedValue<S, C>() }

    return FlagDefinition(
        key = conditional,
        bounds = typedBounds,
        defaultValue = typedDefaultValue,
        salt = salt,
        isActive = isActive
    )
}

/**
 * Converts a SerializableRule to a TargetedValue.
 * The value type is now contained within the SerializableValue wrapper.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : Any, C : Context> SerializableRule.toTargetedValue(): TargetedValue<S, C> {
    val typedValue = value.value.castToType(value.type) as S
    val rule = toRule<C>()
    return rule.targetedBy(typedValue)
}

/**
 * Converts a SerializableRule to a Rule.
 */
private fun <C : Context> SerializableRule.toRule(): Rule<C> {
    return Rule(
        rollout = Rollout.of(rampUp),
        note = note,
        locales = locales.map { AppLocale.valueOf(it) }.toSet(),
        platforms = platforms.map { Platform.valueOf(it) }.toSet(),
        versionRange = versionRange?.toVersionRange() ?: Unbounded
    )
}

/**
 * Converts a SerializableVersionRange to a VersionRange.
 */
private fun SerializableVersionRange.toVersionRange(): VersionRange {
    return when (type) {
        VersionRangeType.UNBOUNDED -> Unbounded
        VersionRangeType.MIN_BOUND -> LeftBound(
            min = min?.toVersion() ?: throw IllegalArgumentException("MIN_BOUND requires min version")
        )
        VersionRangeType.MAX_BOUND -> RightBound(
            max = max?.toVersion() ?: throw IllegalArgumentException("MAX_BOUND requires max version")
        )
        VersionRangeType.MIN_AND_MAX_BOUND -> FullyBound(
            min = min?.toVersion() ?: throw IllegalArgumentException("MIN_AND_MAX_BOUND requires min version"),
            max = max?.toVersion() ?: throw IllegalArgumentException("MIN_AND_MAX_BOUND requires max version")
        )
    }
}

/**
 * Converts a SerializableVersion to a Version.
 */
private fun SerializableVersion.toVersion(): Version {
    return Version(major, minor, patch)
}

/**
 * Casts an Any value to the specified ValueType.
 * Handles numeric conversions between compatible types.
 */
private fun Any.castToType(valueType: ValueType): Any {
    return when (valueType) {
        ValueType.BOOLEAN -> when (this) {
            is Boolean -> this
            else -> throw IllegalArgumentException("Cannot cast $this to Boolean")
        }
        ValueType.STRING -> when (this) {
            is String -> this
            else -> toString()
        }
        ValueType.INT -> when (this) {
            is Int -> this
            is Number -> this.toInt()
            is String -> this.toInt()
            else -> throw IllegalArgumentException("Cannot cast $this to Int")
        }
        ValueType.LONG -> when (this) {
            is Long -> this
            is Number -> this.toLong()
            is String -> this.toLong()
            else -> throw IllegalArgumentException("Cannot cast $this to Long")
        }
        ValueType.DOUBLE -> when (this) {
            is Double -> this
            is Number -> this.toDouble()
            is String -> this.toDouble()
            else -> throw IllegalArgumentException("Cannot cast $this to Double")
        }
    }
}
