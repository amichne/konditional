package io.amichne.konditional.core.dsl

import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.fact.Condition
import io.amichne.konditional.context.fact.FactSchema
import io.amichne.konditional.core.id.StableId

/**
 * Semantic tokens for boolean values in DSL contexts.
 */
const val ENABLED: Boolean = true
const val DISABLED: Boolean = false

/** Yields `true` when the context satisfies [condition]. Same as `rule(true, condition)`. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.enableWhen(
    condition: Condition<S>,
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
) = rule(ENABLED, condition, rollout, note, allowlist)

/** Yields `true` when the context satisfies the inline condition. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.enableWhen(
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
    inlineCondition: @KonditionalDsl Condition<S>,
) = rule(ENABLED, inlineCondition, rollout, note, allowlist)

/** Yields `false` when the context satisfies [condition]. Same as `rule(false, condition)`. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.disableWhen(
    condition: Condition<S>,
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
) = rule(DISABLED, condition, rollout, note, allowlist)

/** Yields `false` when the context satisfies the inline condition. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.disableWhen(
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
    inlineCondition: @KonditionalDsl Condition<S>,
) = rule(DISABLED, inlineCondition, rollout, note, allowlist)

/** Yields `true` for every context inside [rollout]. It takes no condition; see [enableWhen]. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.enable(
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
) = always(ENABLED, rollout, note, allowlist)

/** Yields `false` for every context inside [rollout]. It takes no condition; see [disableWhen]. */
fun <C : Context, S : FactSchema> FlagScope<Boolean, C, S>.disable(
    rollout: Rollout = Rollout.default,
    note: String? = null,
    allowlist: Set<StableId> = emptySet(),
) = always(DISABLED, rollout, note, allowlist)
