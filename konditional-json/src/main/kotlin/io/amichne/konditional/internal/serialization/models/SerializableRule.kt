@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.values.RuleId

/** A portable rule. The conditions array is required, including for an unconditional rule. */
@JsonClass(generateAdapter = true)
internal data class SerializableRule(
    val value: FlagValue<*>,
    val conditions: List<FactSpec>,
    val ruleId: RuleId? = null,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
) {
    fun <T : Any> toSpec(value: T, fallbackRuleId: RuleId): SerializedFlagRuleSpec<T> =
        SerializedFlagRuleSpec(
            value = value,
            conditions = conditions,
            ruleId = ruleId ?: fallbackRuleId,
            rampUp = rampUp,
            rampUpAllowlist = rampUpAllowlist,
            note = note,
        )

    companion object {
        fun fromSpec(rule: SerializedFlagRuleSpec<*>): SerializableRule =
            SerializableRule(
                value = FlagValue.from(rule.value),
                conditions = rule.conditions,
                ruleId = rule.ruleId,
                rampUp = rule.rampUp,
                rampUpAllowlist = rule.rampUpAllowlist,
                note = rule.note,
            )
    }
}
