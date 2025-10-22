package io.amichne.konditional.serialization

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Condition
import io.amichne.konditional.core.Conditional
import io.amichne.konditional.core.Flags
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.Surjection
import io.amichne.konditional.rules.Surjection.Companion.boundedBy
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.rules.versions.LeftBound
import io.amichne.konditional.rules.versions.RightBound
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

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
        flagEntry.condition.toSerializable(conditional.key)
    }
    return SerializableSnapshot(serializableFlags)
}

/**
 * Converts a Condition to a SerializableFlag.
 */
private fun <S : Any, C : Context> Condition<S, C>.toSerializable(flagKey: String): SerializableFlag {
    return SerializableFlag(
        key = flagKey,
        valueType = defaultValue.toValueType(),
        defaultValue = defaultValue,
        salt = salt,
        isActive = isActive,
        rules = bounds.map { it.toSerializable() }
    )
}

/**
 * Converts a Surjection to a SerializableRule.
 */
private fun <S : Any, C : Context> Surjection<S, C>.toSerializable(): SerializableRule {
    return SerializableRule(
        value = value,
        rampUp = rule.rampUp.value,
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
            type = VersionRangeType.LEFT_BOUND,
            min = min.toSerializableVersion()
        )
        is RightBound -> SerializableVersionRange(
            type = VersionRangeType.RIGHT_BOUND,
            max = max.toSerializableVersion()
        )
        is FullyBound -> SerializableVersionRange(
            type = VersionRangeType.FULLY_BOUND,
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
    val condition = toCondition(conditional as Conditional<Any, Context>)
    val flagEntry = Flags.FlagEntry(condition)
    return conditional to flagEntry
}

/**
 * Converts a SerializableFlag to a Condition.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : Any, C : Context> SerializableFlag.toCondition(
    conditional: Conditional<S, C>
): Condition<S, C> {
    val typedDefaultValue = defaultValue.castToType(valueType) as S
    val typedBounds = rules.map { it.toSurjection<S, C>(valueType) }

    return Condition(
        key = conditional,
        bounds = typedBounds,
        defaultValue = typedDefaultValue,
        salt = salt,
        isActive = isActive
    )
}

/**
 * Converts a SerializableRule to a Surjection.
 */
@Suppress("UNCHECKED_CAST")
private fun <S : Any, C : Context> SerializableRule.toSurjection(valueType: ValueType): Surjection<S, C> {
    val typedValue = value.castToType(valueType) as S
    val rule = toRule<C>()
    return rule.boundedBy(typedValue)
}

/**
 * Converts a SerializableRule to a Rule.
 */
private fun <C : Context> SerializableRule.toRule(): Rule<C> {
    return Rule(
        rampUp = RampUp.of(rampUp),
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
        VersionRangeType.LEFT_BOUND -> LeftBound(
            min = min?.toVersion() ?: throw IllegalArgumentException("LEFT_BOUND requires min version")
        )
        VersionRangeType.RIGHT_BOUND -> RightBound(
            max = max?.toVersion() ?: throw IllegalArgumentException("RIGHT_BOUND requires max version")
        )
        VersionRangeType.FULLY_BOUND -> FullyBound(
            min = min?.toVersion() ?: throw IllegalArgumentException("FULLY_BOUND requires min version"),
            max = max?.toVersion() ?: throw IllegalArgumentException("FULLY_BOUND requires max version")
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
