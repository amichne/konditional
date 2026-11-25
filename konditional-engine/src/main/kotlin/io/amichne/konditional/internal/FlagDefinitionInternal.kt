@file:OptIn(ExperimentalStdlibApi::class)

package io.amichne.konditional.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBySerialized
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.rules.targeting.TargetingSchema
import io.amichne.konditional.rules.targeting.axesOrEmpty
import io.amichne.konditional.rules.targeting.localesOrEmpty
import io.amichne.konditional.rules.targeting.platformsOrEmpty
import io.amichne.konditional.rules.targeting.versionRangeOrNull
import io.amichne.konditional.rules.versions.Unbounded
import io.amichne.konditional.rules.versions.VersionRange
import io.amichne.konditional.values.RuleId

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
    val type: SerializedRuleValueType = SerializedRuleValueType.STATIC,
    val ruleId: RuleId = RuleId.unspecified,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
    val locales: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val versionRange: VersionRange? = null,
    val axes: Map<String, Set<String>> = emptyMap(),
    val targeting: SerializedTargeting? = null,
)

@KonditionalInternalApi
enum class SerializedRuleValueType {
    STATIC,
    CONTEXTUAL,
}

@KonditionalInternalApi
fun <T : Any, C : Context, M : Namespace> flagDefinitionFromSerialized(
    feature: Feature<T, C, M>,
    defaultValue: T,
    rules: List<SerializedFlagRuleSpec<T>>,
    metadata: SerializedFlagDefinitionMetadata = SerializedFlagDefinitionMetadata(),
    targetingSchema: TargetingSchema<C> = TargetingSchema(),
): ParseResult<FlagDefinition<T, C, M>> {
    val values = ArrayList<ConditionalValue<T, C>>(rules.size)
    for (spec in rules) {
        val targeting = when (val bound = spec.targeting?.bindRule(targetingSchema)) {
            null -> spec.legacyTargeting<C>()
            is ParseResult.Success -> bound.value
            is ParseResult.Failure -> return bound
        }
        values += Rule(
            rampUp = RampUp.of(spec.rampUp),
            rampUpAllowlist = spec.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
            note = spec.note,
            targeting = targeting,
            ruleId = spec.ruleId,
        ).targetedBySerialized(spec.value, spec.type)
    }
    return ParseResult.Success(FlagDefinition(
        feature = feature,
        values = java.util.List.copyOf(values),
        defaultValue = defaultValue,
        salt = metadata.salt,
        isActive = metadata.isActive,
        rampUpAllowlist = metadata.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
        targetingSchema = targetingSchema,
    ))
}

private fun <C : Context> SerializedFlagRuleSpec<*>.legacyTargeting(): Targeting.All<C> =
    Targeting.All(buildList {
        if (locales.isNotEmpty()) add(Targeting.locale(locales.frozenIds()))
        if (platforms.isNotEmpty()) add(Targeting.platform(platforms.frozenIds()))
        versionRange?.takeIf { it != Unbounded }?.let { add(Targeting.version(it)) }
        axes.forEach { (axisId, allowedIds) -> add(Targeting.Axis(axisId, allowedIds.frozenIds())) }
    })

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
        val staticValue = cv.staticValueOrNull()
        val type = cv.serializedValueType
        val value = staticValue ?: defaultValue
        val targeting = cv.rule.targeting
        val common = SerializedFlagRuleSpec(
            value = value,
            type = type,
            ruleId = cv.rule.ruleId,
            rampUp = cv.rule.rampUp.value,
            rampUpAllowlist = cv.rule.rampUpAllowlist.mapTo(linkedSetOf()) { it.id },
            note = cv.rule.note,
        )
        val expression = targeting.serialize()
        val legacy = common.copy(
            locales = targeting.localesOrEmpty(),
            platforms = targeting.platformsOrEmpty(),
            versionRange = targeting.versionRangeOrNull() ?: Unbounded,
            axes = targeting.axesOrEmpty(),
        )
        // Emit the legacy shape only if its reader can reproduce the entire tree, in order.
        when {
            legacy.legacyTargeting<Context>().serialize() == expression -> legacy
            type == SerializedRuleValueType.STATIC -> common.copy(targeting = expression)
            else -> throw KonditionalBoundaryFailure(ParseError.invalidSnapshot(
                "Feature '${feature.key}' rule '${cv.rule.ruleId}' needs a targeting expression and has a contextual value",
            ))
        }
    }
