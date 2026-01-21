@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.instance

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.toSerializedMetadata
import io.amichne.konditional.internal.toSerializedRules
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.values.FeatureId

@ConsistentCopyVisibility
data class ConfigurationDiff internal constructor(
    val before: ConfigurationMetadata,
    val after: ConfigurationMetadata,
    val added: List<FlagChange.FlagSnapshot>,
    val removed: List<FlagChange.FlagSnapshot>,
    val changed: List<FlagChange>,
) {
    companion object {
        private fun FlagDefinition<*, *, *>.toSnapshot(feature: Feature<*, *, *>): FlagChange.FlagSnapshot {
            val metadata = toSerializedMetadata()
            return FlagChange.FlagSnapshot(
                id = feature.id,
                key = feature.key,
                isActive = metadata.isActive,
                salt = metadata.salt,
                defaultValue = ConfigValue.from(defaultValue),
                rules =
                    toSerializedRules().map { spec ->
                        FlagChange.FlagSnapshot.RuleValueSnapshot(
                            rule = spec.toSnapshot(),
                            value = ConfigValue.from(spec.value),
                        )
                    },
            )
        }

        private fun SerializedFlagRuleSpec<Any>.toSnapshot(): FlagChange.FlagSnapshot.RuleValueSnapshot.RuleSnapshot {
            val baseSpecificity =
                (if (locales.isNotEmpty()) 1 else 0) +
                    (if (platforms.isNotEmpty()) 1 else 0) +
                    (if ((versionRange ?: Unbounded).hasBounds()) 1 else 0) +
                    axes.size

            return FlagChange.FlagSnapshot.RuleValueSnapshot.RuleSnapshot(
                note = note,
                rollout = RampUp.of(rampUp),
                locales = locales,
                platforms = platforms,
                versionRange = versionRange ?: Unbounded,
                axes = axes,
                baseSpecificity = baseSpecificity,
                extensionSpecificity = 0,
                totalSpecificity = baseSpecificity,
                extensionClassName = null,
            )
        }

        fun between(before: Configuration, after: Configuration): ConfigurationDiff =
            ConfigurationDiff(
                before = before.metadata,
                after = after.metadata,
                added =
                    (after.flags.keys - before.flags.keys)
                        .sortedBy { it.id }
                        .map { after.flags.getValue(it).toSnapshot(it) },
                removed =
                    (before.flags.keys - after.flags.keys)
                        .sortedBy { it.id }
                        .map { before.flags.getValue(it).toSnapshot(it) },
                changed =
                    (before.flags.keys intersect after.flags.keys)
                        .sortedBy { it.id }
                        .mapNotNull { feature ->
                            val left = before.flags.getValue(feature).toSnapshot(feature)
                            val right = after.flags.getValue(feature).toSnapshot(feature)
                            if (left == right) null else FlagChange(feature.id, feature.key, left, right)
                        },
            )
    }

    @ConsistentCopyVisibility
    data class FlagChange internal constructor(
        val id: FeatureId,
        val key: String,
        val before: FlagSnapshot,
        val after: FlagSnapshot,
    ) {
        @ConsistentCopyVisibility
        data class FlagSnapshot internal constructor(
            val id: FeatureId,
            val key: String,
            val isActive: Boolean,
            val salt: String,
            val defaultValue: ConfigValue,
            val rules: List<RuleValueSnapshot>,
        ) {
            @ConsistentCopyVisibility
            data class RuleValueSnapshot internal constructor(
                val rule: RuleSnapshot,
                val value: ConfigValue,
            ) {
                @ConsistentCopyVisibility
                data class RuleSnapshot internal constructor(
                    val note: String?,
                    val rollout: RampUp,
                    val locales: Set<String>,
                    val platforms: Set<String>,
                    val versionRange: VersionRange,
                    val axes: Map<String, Set<String>>,
                    val baseSpecificity: Int,
                    val extensionSpecificity: Int,
                    val totalSpecificity: Int,
                    val extensionClassName: String?,
                )
            }
        }
    }
}
