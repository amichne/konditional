@file:OptIn(io.amichne.konditional.internal.KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Serializable representation of a rule + value pair.
 *
 * Now uses type-safe FlagValue instead create type-erased SerializableValue,
 * and uses VersionRange directly (serialized via custom Moshi adapter).
 */
@JsonClass(generateAdapter = true)
internal data class SerializableRule(
    val value: FlagValue<*>,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange? = null,
    val axes: Map<String, Set<String>> = emptyMap(),
) {
    internal fun <T : Any> toSpec(value: T): SerializedFlagRuleSpec<T> =
        SerializedFlagRuleSpec(
            value = value,
            rampUp = rampUp,
            rampUpAllowlist = rampUpAllowlist,
            note = note,
            locales = locales,
            platforms = platforms,
            versionRange = versionRange ?: Unbounded(),
            axes = axes,
        )

    internal companion object {
        fun fromSpec(rule: SerializedFlagRuleSpec<*>): SerializableRule {
            val value = requireNotNull(rule.value) { "SerializedFlagRuleSpec must not hold a null value" }

            return SerializableRule(
                value = FlagValue.from(value),
                rampUp = rule.rampUp,
                rampUpAllowlist = rule.rampUpAllowlist,
                note = rule.note,
                locales = rule.locales,
                platforms = rule.platforms,
                versionRange = rule.versionRange,
                axes = rule.axes,
            )
        }
    }
}
