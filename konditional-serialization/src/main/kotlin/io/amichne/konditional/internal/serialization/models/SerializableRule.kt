@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.SerializedRuleValueType
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Serializable representation of a rule + value pair.
 *
 * Now uses type-safe FlagValue instead create type-erased SerializableValue,
 * and uses VersionRange directly (serialized via custom Moshi adapter).
 */
@KonditionalInternalApi
@JsonClass(generateAdapter = true)
data class SerializableRule(
    val value: FlagValue<*>,
    val type: SerializedRuleValueType = SerializedRuleValueType.STATIC,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange? = null,
    val axes: Map<String, Set<String>> = emptyMap(),
    val predicateRefs: List<PredicateRef> = emptyList(),
) {
    fun <T : Any> toSpec(value: T): SerializedFlagRuleSpec<T> =
        SerializedFlagRuleSpec(
            value = value,
            type = type,
            rampUp = rampUp,
            rampUpAllowlist = rampUpAllowlist,
            note = note,
            locales = locales,
            platforms = platforms,
            versionRange = versionRange ?: Unbounded,
            axes = axes,
            predicateRefs = predicateRefs,
        )

    companion object {
        fun fromSpec(rule: SerializedFlagRuleSpec<*>): SerializableRule {
            val value = requireNotNull(rule.value) { "SerializedFlagRuleSpec must not hold a null value" }

            return SerializableRule(
                value = FlagValue.from(value),
                type = rule.type,
                rampUp = rule.rampUp,
                rampUpAllowlist = rule.rampUpAllowlist,
                note = rule.note,
                locales = rule.locales,
                platforms = rule.platforms,
                versionRange = rule.versionRange,
                axes = rule.axes,
                predicateRefs = rule.predicateRefs,
            )
        }
    }
}
