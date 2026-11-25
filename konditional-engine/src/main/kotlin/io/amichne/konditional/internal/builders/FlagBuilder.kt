@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.builders

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactExpression
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.FlagScope
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.dsl.rules.RuleValueResolver
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.HexId
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.rules.ConditionalValue
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.targeting.Targeting
import io.amichne.konditional.values.RuleId

/**
 * Internal implementation of [FlagScope].
 *
 * Binds each rule's condition to the namespace's schema exactly once, in declaration order,
 * and keeps only the resulting expression.
 */
@KonditionalDsl
@KonditionalInternalApi
internal class FlagBuilder<T : Any, C : Context, S : FactSchema, M : Namespace<S>>(
    override val default: T,
    private val feature: Feature<T, C, M>,
) : FlagScope<T, C, S> {
    private val facts: S = feature.namespace.facts
    private val values = mutableListOf<ConditionalValue<T, C>>()
    private val rolloutAllowlist: LinkedHashSet<HexId> = linkedSetOf()
    private var nextRuleOrdinal: Int = 0

    private var salt: String = "v1"
    private var isActive: Boolean = true

    override fun active(block: () -> Boolean) {
        isActive = block()
    }

    override fun allowlist(vararg stableIds: StableId) {
        rolloutAllowlist += stableIds.map { it.hexId }
    }

    override fun salt(value: String) {
        salt = value
    }

    override fun rule(
        value: T,
        condition: Condition<S>,
        rollout: Rollout,
        note: String?,
        allowlist: Set<StableId>,
    ) {
        values += newRule(condition, rollout, note, allowlist).targetedBy(value)
    }

    override fun always(
        value: T,
        rollout: Rollout,
        note: String?,
        allowlist: Set<StableId>,
    ) {
        values += newRule(null, rollout, note, allowlist).targetedBy(value)
    }

    override fun ruleResolved(
        condition: Condition<S>,
        rollout: Rollout,
        note: String?,
        allowlist: Set<StableId>,
        value: RuleValueResolver<C, T>,
    ) {
        values += newRule(condition, rollout, note, allowlist).targetedBy(value)
    }

    override fun alwaysResolved(
        rollout: Rollout,
        note: String?,
        allowlist: Set<StableId>,
        value: RuleValueResolver<C, T>,
    ) {
        values += newRule(null, rollout, note, allowlist).targetedBy(value)
    }

    @KonditionalInternalApi
    fun build(): FlagDefinition<T, C, M> =
        FlagDefinition(
            feature = feature,
            bounds = values,
            defaultValue = default,
            salt = salt,
            isActive = isActive,
            rolloutAllowlist = rolloutAllowlist,
        )

    private fun newRule(
        condition: Condition<S>?,
        rollout: Rollout,
        note: String?,
        allowlist: Set<StableId>,
    ): Rule<C> {
        val ordinal = nextRuleOrdinal++
        val leaves = listOfNotNull(condition?.let { Targeting.Facts(bind(it, ordinal)) })
        return Rule(
            rollout = rollout,
            rolloutAllowlist = allowlist.mapTo(linkedSetOf()) { it.hexId },
            note = note,
            targeting = Targeting.All(leaves),
            ruleId = RuleId.forFeatureRule(feature.id, ordinal),
        )
    }

    private fun bind(condition: Condition<S>, ordinal: Int): FactExpression {
        val expression = facts.condition()
        val foreign = facts.foreignFacts(expression).distinct()
        require(foreign.isEmpty()) {
            "Feature '${feature.key}', rule $ordinal: the condition uses " +
                foreign.joinToString { "'${it.key.id}'" } +
                " from a schema that namespace '${feature.namespace.id}' does not declare. " +
                "A condition must read every fact from its receiver."
        }
        return expression
    }
}
