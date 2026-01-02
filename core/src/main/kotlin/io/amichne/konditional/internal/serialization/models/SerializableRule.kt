package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Serializable representation of a ConditionalValue (rule + value pair).
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
    internal fun <C : Context> toRule(): Rule<C> =
        Rule(
            rampUp = RampUp.of(rampUp),
            rolloutAllowlist = rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
            note = note,
            locales = locales.toSet(),
            platforms = platforms.toSet(),
            versionRange = (versionRange ?: Unbounded()),
            axisConstraints = axes.map { (axisId, allowedIds) -> AxisConstraint(axisId, allowedIds) },
        )

    internal companion object {
        fun from(conditionalValue: ConditionalValue<*, *>): SerializableRule {
            val value = requireNotNull(conditionalValue.value) {
                "ConditionalValue must not hold a null value"
            }

            return SerializableRule(
                value = FlagValue.from(value),
                rampUp = conditionalValue.rule.rampUp.value,
                rampUpAllowlist = conditionalValue.rule.rampUpAllowlist.mapTo(linkedSetOf()) { it.id },
                note = conditionalValue.rule.note,
                locales = conditionalValue.rule.targeting.locales.toSet(),
                platforms = conditionalValue.rule.targeting.platforms.toSet(),
                versionRange = conditionalValue.rule.targeting.versionRange,
                axes = conditionalValue.rule.targeting.axisConstraints.associate { it.axisId to it.allowedIds },
            )
        }
    }
}
