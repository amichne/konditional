package io.amichne.konditional.internal

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.fact.FactSpec
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.RuleId

/** Configuration metadata shared with snapshot codecs. */
@KonditionalInternalApi
data class SerializedFlagDefinitionMetadata(
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rampUpAllowlist: Set<String> = emptySet(),
)

/** One ordered conjunction and the value selected when it matches. */
@KonditionalInternalApi
data class SerializedFlagRuleSpec<T : Any>(
    val value: T,
    val conditions: List<FactSpec>,
    val ruleId: RuleId = RuleId.unspecified,
    val rampUp: Double = 100.0,
    val rampUpAllowlist: Set<String> = emptySet(),
    val note: String? = null,
)

/** Resolve every expression against the destination namespace before returning a definition. */
@KonditionalInternalApi
fun <T : Any, C : Context, M : Namespace> flagDefinitionFromSerialized(
    feature: Feature<T, C, M>,
    defaultValue: T,
    rules: List<SerializedFlagRuleSpec<T>>,
    metadata: SerializedFlagDefinitionMetadata = SerializedFlagDefinitionMetadata(),
): Result<FlagDefinition<T, C, M>> {
    val bounds = rules.map { spec ->
        val targets = mutableListOf<Targeting<C>>()
        for (condition in spec.conditions) {
            when (val parsed = feature.namespace.factSchema.resolve(condition)) {
                is ParseResult.Failure -> return parseFailure(parsed.error)
                is ParseResult.Success -> targets.add(Targeting.Facts(parsed.value))
            }
        }
        Rule(
            rampUp = RampUp.of(spec.rampUp),
            rampUpAllowlist = spec.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
            note = spec.note,
            targeting = Targeting.All(targets),
            ruleId = spec.ruleId,
        ).targetedBy(spec.value)
    }
    return Result.success(
        FlagDefinition(
            feature = feature,
            bounds = bounds,
            defaultValue = defaultValue,
            salt = metadata.salt,
            isActive = metadata.isActive,
            rampUpAllowlist = metadata.rampUpAllowlist.mapTo(linkedSetOf()) { HexId(it) },
        ),
    )
}

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
        val value = cv.staticValueOrNull() ?: throw KonditionalBoundaryFailure(
            ParseError.invalidSnapshot("Cannot export feature '${feature.id}', rule '${cv.rule.ruleId}': contextual value resolver."),
        )
        val conditions = cv.rule.targeting.targets.map { condition ->
            when (condition) {
                is Targeting.Facts -> condition.expression.spec
                is Targeting.Custom, is Targeting.Guarded<*, *>, is Targeting.AnyOf, is Targeting.All ->
                    throw KonditionalBoundaryFailure(ParseError.invalidSnapshot(
                        "Cannot export feature '${feature.id}', rule '${cv.rule.ruleId}': " +
                            "the snapshot cannot preserve the ${condition::class.simpleName} condition.",
                    ))
            }
        }
        SerializedFlagRuleSpec(
            value = value,
            conditions = conditions,
            ruleId = cv.rule.ruleId,
            rampUp = cv.rule.rampUp.value,
            rampUpAllowlist = cv.rule.rampUpAllowlist.mapTo(linkedSetOf()) { it.id },
            note = cv.rule.note,
        )
    }
