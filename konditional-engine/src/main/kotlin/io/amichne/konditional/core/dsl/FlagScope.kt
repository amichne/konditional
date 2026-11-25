package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.core.dsl.rules.RuleValueResolver
import io.amichne.konditional.core.id.StableId

/**
 * Declares one flag: its default and the rules that can override it.
 *
 * A rule pairs an outcome with at most one [Condition] over the namespace's facts [S]. The
 * condition is bound to the namespace's schema when the flag is declared. If the bound expression
 * uses a fact that another schema declared, the declaration fails before the flag is observable.
 *
 * Every rule setting is an argument. No rule form takes a `Unit` block, so a comparison written
 * where a rule expects its condition cannot be discarded.
 *
 * ```kotlin
 * val acceptDelivery by boolean<Reading>(default = false) {
 *     enableWhen(chilledDelivery, rollout = Rollout.of(25))
 *     disableWhen { zone.oneOf(Zone.QUARANTINE) }
 * }
 * ```
 *
 * @param T The flag's value type
 * @param C The context type the flag evaluates against
 * @param S The namespace's fact schema
 */
@KonditionalDsl
@Suppress("TooManyFunctions")
interface FlagScope<T : Any, C : Context, S : FactSchema> {
    val default: T

    fun active(block: () -> Boolean)

    /**
     * Lets these stable IDs bypass rollout for every rule in this flag.
     * An allowlisted ID still has to match the rule's condition.
     */
    fun allowlist(vararg stableIds: StableId)

    /** Sets the salt used for rollout bucketing. Changing it redistributes stable IDs. Default "v1". */
    fun salt(value: String)

    /** Yields [value] when the context satisfies [condition] and falls inside [rollout]. */
    fun rule(
        value: T,
        condition: Condition<S>,
        rollout: Rollout = Rollout.default,
        note: String? = null,
        allowlist: Set<StableId> = emptySet(),
    )

    /** The same rule with the condition written inline: `rule(value) { channel.oneOf(PILOT) }`. */
    fun rule(
        value: T,
        rollout: Rollout = Rollout.default,
        note: String? = null,
        allowlist: Set<StableId> = emptySet(),
        inlineCondition: @KonditionalDsl Condition<S>,
    ) = rule(value, inlineCondition, rollout, note, allowlist)

    /** Yields [value] for every context that falls inside [rollout]. Ranks below every conditional rule. */
    fun always(
        value: T,
        rollout: Rollout = Rollout.default,
        note: String? = null,
        allowlist: Set<StableId> = emptySet(),
    )

    /**
     * Like [rule], with the value computed from the evaluation context after the rule matches.
     * A snapshot cannot carry a computed value, so exporting this flag fails.
     */
    fun ruleResolved(
        condition: Condition<S>,
        rollout: Rollout = Rollout.default,
        note: String? = null,
        allowlist: Set<StableId> = emptySet(),
        value: RuleValueResolver<C, T>,
    )

    /** Like [always], with the value computed from the evaluation context. */
    fun alwaysResolved(
        rollout: Rollout = Rollout.default,
        note: String? = null,
        allowlist: Set<StableId> = emptySet(),
        value: RuleValueResolver<C, T>,
    )
}
