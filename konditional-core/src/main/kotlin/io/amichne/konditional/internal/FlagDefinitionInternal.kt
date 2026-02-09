@file:OptIn(ExperimentalStdlibApi::class)

package io.amichne.konditional.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.RuleValue
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange

/**
 * Internal contracts for encoding/decoding flag definitions across Konditional modules.
 *
 * These are deliberately shaped around primitives and stable public types so sibling modules can
 * integrate without `-Xfriend-paths`, while still keeping these contracts explicitly non-public.
 */
@KonditionalInternalApi
data class SerializedFlagDefinitionMetadata(
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rampUpAllowlist: Set<String> = emptySet(),
)

@KonditionalInternalApi
@Suppress("LongParameterList")
data class SerializedFlagRuleSpec<T : Any>(
    val value: T,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange? = null,
    val axes: Map<String, Set<String>> = emptyMap(),
)

@KonditionalInternalApi
fun <T : Any, C : Context, M : Namespace> flagDefinitionFromSerialized(
    feature: Feature<T, C, M>,
    defaultValue: T,
    rules: List<SerializedFlagRuleSpec<T>>,
    metadata: SerializedFlagDefinitionMetadata = SerializedFlagDefinitionMetadata(),
): FlagDefinition<T, C, M> =
    FlagDefinition(
        feature = feature,
        bounds =
            rules.map { spec ->
                Rule<C>(
                    rampUp = RampUp.of(spec.rampUp),
                    rolloutAllowlist = spec.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
                    note = spec.note,
                    locales = spec.locales,
                    platforms = spec.platforms,
                    versionRange = spec.versionRange ?: Unbounded,
                    axisConstraints =
                        spec.axes.map { (axisId, allowedIds) ->
                            AxisConstraint(axisId, allowedIds)
                        },
                ).targetedBy(spec.value)
            },
        defaultValue = defaultValue,
        salt = metadata.salt,
        isActive = metadata.isActive,
        rampUpAllowlist = metadata.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
    )

@KonditionalInternalApi
fun FlagDefinition<*, *, *>.toSerializedMetadata(): SerializedFlagDefinitionMetadata =
    SerializedFlagDefinitionMetadata(
        salt = salt,
        isActive = isActive,
        rampUpAllowlist = rampUpAllowlist.mapTo(linkedSetOf()) { it.id },
    )

@KonditionalInternalApi
fun FlagDefinition<*, *, *>.toSerializedRules(): List<SerializedFlagRuleSpec<Any>> =
    values.map { cv ->
        when (val value = cv.value) {
            is RuleValue.Fixed -> SerializedFlagRuleSpec(
                value = value.value,
                rampUp = cv.rule.rampUp.value,
                rampUpAllowlist = cv.rule.rampUpAllowlist.mapTo(linkedSetOf()) { it.id },
                note = cv.rule.note,
                locales = cv.rule.targeting.locales.toSet(),
                platforms = cv.rule.targeting.platforms.toSet(),
                versionRange = cv.rule.targeting.versionRange,
                axes = cv.rule.targeting.axisConstraints.associate { it.axisId to it.allowedIds },
            )
            is RuleValue.Contextual -> error("Contextual rule values cannot be serialized.")
        }
    }
