package io.amichne.konditional.core.instance

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rampup
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.konditional.core.types.toJsonValue
import io.amichne.konditional.core.types.toPrimitiveValue
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.Placeholder
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.values.Identifier
import io.amichne.kontracts.schema.ObjectSchema

@ConsistentCopyVisibility
data class ConfigurationDiff internal constructor(
    val before: ConfigurationMetadata,
    val after: ConfigurationMetadata,
    val added: List<FlagSnapshot>,
    val removed: List<FlagSnapshot>,
    val changed: List<FlagChange>,
) {
    companion object {
        fun between(before: Configuration, after: Configuration): ConfigurationDiff = ConfigurationDiff(
            before = before.metadata,
            after = after.metadata,
            added = (after.flags.keys - before.flags.keys)
                .sortedBy { it.id }
                .map { after.flags.getValue(it).toSnapshot(it) },
            removed = (before.flags.keys - after.flags.keys)
                .sortedBy { it.id }
                .map { before.flags.getValue(it).toSnapshot(it) },
            changed = (before.flags.keys intersect after.flags.keys)
                .sortedBy { it.id }
                .mapNotNull { feature ->
                    val left = before.flags.getValue(feature).toSnapshot(feature)
                    val right = after.flags.getValue(feature).toSnapshot(feature)
                    if (left == right) null else FlagChange(
                        id = feature.id,
                        key = feature.key,
                        before = left,
                        after = right,
                    )
                },
        )
    }
}

@ConsistentCopyVisibility
data class FlagChange internal constructor(
    val id: Identifier,
    val key: String,
    val before: FlagSnapshot,
    val after: FlagSnapshot,
)

@ConsistentCopyVisibility
data class FlagSnapshot internal constructor(
    val id: Identifier,
    val key: String,
    val isActive: Boolean,
    val salt: String,
    val defaultValue: ConfigValue,
    val rules: List<RuleValueSnapshot>,
)

@ConsistentCopyVisibility
data class RuleValueSnapshot internal constructor(
    val rule: RuleSnapshot,
    val value: ConfigValue,
)

@ConsistentCopyVisibility
data class RuleSnapshot internal constructor(
    val note: String?,
    val rollout: Rampup,
    val locales: Set<AppLocale>,
    val platforms: Set<Platform>,
    val versionRange: VersionRange,
    val axes: Map<String, Set<String>>,
    val baseSpecificity: Int,
    val extensionSpecificity: Int,
    val totalSpecificity: Int,
    val extensionClassName: String?,
)

sealed interface ConfigValue {
    @ConsistentCopyVisibility
    data class BooleanValue internal constructor(val value: Boolean) : ConfigValue

    @ConsistentCopyVisibility
    data class StringValue internal constructor(val value: String) : ConfigValue

    @ConsistentCopyVisibility
    data class IntValue internal constructor(val value: Int) : ConfigValue

    @ConsistentCopyVisibility
    data class DoubleValue internal constructor(val value: Double) : ConfigValue

    @ConsistentCopyVisibility
    data class EnumValue internal constructor(
        val enumClassName: String,
        val constantName: String,
    ) : ConfigValue

    @ConsistentCopyVisibility
    data class DataClassValue internal constructor(
        val dataClassName: String,
        val fields: Map<String, Any?>,
    ) : ConfigValue

    @ConsistentCopyVisibility
    data class Opaque internal constructor(
        val typeName: String,
        val debug: String,
    ) : ConfigValue

    companion object {
        fun from(value: Any): ConfigValue = when (value) {
            is Boolean -> BooleanValue(value)
            is String -> StringValue(value)
            is Int -> IntValue(value)
            is Double -> DoubleValue(value)
            is Enum<*> -> EnumValue(value.javaClass.name, value.name)
            is KotlinEncodeable<*> -> {
                @Suppress("UNCHECKED_CAST")
                val encodeable = value as KotlinEncodeable<ObjectSchema>
                val json = encodeable.toJsonValue()
                DataClassValue(
                    dataClassName = value::class.java.name,
                    fields = json.fields.mapValues { (_, v) -> v.toPrimitiveValue() },
                )
            }
            else -> Opaque(
                typeName = value::class.qualifiedName ?: value::class.simpleName ?: "unknown",
                debug = value.toString(),
            )
        }
    }
}

private fun FlagDefinition<*, *, *>.toSnapshot(feature: Feature<*, *, *>): FlagSnapshot =
    FlagSnapshot(
        id = feature.id,
        key = feature.key,
        isActive = isActive,
        salt = salt,
        defaultValue = ConfigValue.from(defaultValue),
        rules = valuesByPrecedence.map { conditional ->
            RuleValueSnapshot(
                rule = conditional.rule.toSnapshot(),
                value = ConfigValue.from(conditional.value),
            )
        },
    )

private fun Rule<*>.toSnapshot(): RuleSnapshot {
    val base = baseEvaluable
    val baseSpecificity =
        (if (base.locales.isNotEmpty()) 1 else 0) +
            (if (base.platforms.isNotEmpty()) 1 else 0) +
            (if (base.versionRange.hasBounds()) 1 else 0) +
            base.axisConstraints.size

    val extensionSpecificity = extension.specificity()
    val extensionClassName = when (extension) {
        Placeholder -> null
        else -> extension::class.qualifiedName
    }

    return RuleSnapshot(
        note = note,
        rollout = rollout,
        locales = base.locales,
        platforms = base.platforms,
        versionRange = base.versionRange.takeIf { it.hasBounds() } ?: Unbounded(),
        axes = base.axisConstraints.associate { it.axisId to it.allowedIds },
        baseSpecificity = baseSpecificity,
        extensionSpecificity = extensionSpecificity,
        totalSpecificity = baseSpecificity + extensionSpecificity,
        extensionClassName = extensionClassName,
    )
}
