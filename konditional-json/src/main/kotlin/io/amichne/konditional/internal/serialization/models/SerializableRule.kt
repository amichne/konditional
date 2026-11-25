@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.SerializedRuleValueType
import io.amichne.konditional.internal.SerializedTargeting
import io.amichne.konditional.internal.serialization.adapters.TargetingField
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.values.RuleId

/**
 * Serializable representation of a rule + value pair.
 *
 * Now uses type-safe FlagValue instead create type-erased SerializableValue,
 * and uses VersionRange directly (serialized via custom Moshi adapter).
 */
@KonditionalInternalApi
@JsonClass(generateAdapter = true)
internal data class SerializableRule(
    val value: FlagValue<*>,
    val type: RuleFormat = RuleFormat.STATIC,
    val ruleId: RuleId? = null,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    @TargetingField val locales: Set<String>? = if (type == RuleFormat.TARGETING_V1) null else emptySet(),
    @TargetingField val platforms: Set<String>? = if (type == RuleFormat.TARGETING_V1) null else emptySet(),
    @TargetingField val versionRange: VersionRange? = null,
    @TargetingField val axes: Map<String, Set<String>>? = if (type == RuleFormat.TARGETING_V1) null else emptyMap(),
    @TargetingField val targeting: SerializedTargeting? = null,
) {
    init {
        when (type) {
            RuleFormat.TARGETING_V1 -> {
                requireNotNull(targeting) { "TARGETING_V1 requires a targeting expression" }
                require(locales == null && platforms == null && versionRange == null && axes == null) {
                    "TARGETING_V1 cannot include flat targeting fields"
                }
            }
            RuleFormat.STATIC, RuleFormat.CONTEXTUAL -> {
                require(targeting == null) { "A targeting expression requires the TARGETING_V1 rule type" }
                require(locales != null && platforms != null && axes != null) { "Flat targeting fields cannot be null" }
            }
        }
    }

    fun <T : Any> toSpec(
        value: T,
        fallbackRuleId: RuleId,
    ): SerializedFlagRuleSpec<T> =
        SerializedFlagRuleSpec(
            value = value,
            type = type.valueType,
            ruleId = ruleId ?: fallbackRuleId,
            rampUp = rampUp,
            rampUpAllowlist = rampUpAllowlist,
            note = note,
            locales = locales.orEmpty(),
            platforms = platforms.orEmpty(),
            versionRange = versionRange ?: Unbounded,
            axes = axes.orEmpty(),
            targeting = targeting,
        )

    companion object {
        fun fromSpec(rule: SerializedFlagRuleSpec<*>): SerializableRule {
            val value = requireNotNull(rule.value) { "SerializedFlagRuleSpec must not hold a null value" }
            val format = when (rule.type) {
                SerializedRuleValueType.STATIC -> if (rule.targeting == null) RuleFormat.STATIC else RuleFormat.TARGETING_V1
                SerializedRuleValueType.CONTEXTUAL -> RuleFormat.CONTEXTUAL
            }
            val flat = format != RuleFormat.TARGETING_V1
            return SerializableRule(
                value = FlagValue.from(value),
                type = format,
                ruleId = rule.ruleId,
                rampUp = rule.rampUp,
                rampUpAllowlist = rule.rampUpAllowlist,
                note = rule.note,
                locales = rule.locales.takeIf { flat },
                platforms = rule.platforms.takeIf { flat },
                versionRange = rule.versionRange.takeIf { flat },
                axes = rule.axes.takeIf { flat },
                targeting = rule.targeting,
            )
        }
    }
}

/** A new discriminator makes readers that only understand flat rules reject expression snapshots. */
internal enum class RuleFormat(val valueType: SerializedRuleValueType) {
    STATIC(SerializedRuleValueType.STATIC),
    CONTEXTUAL(SerializedRuleValueType.CONTEXTUAL),
    TARGETING_V1(SerializedRuleValueType.STATIC),
}
