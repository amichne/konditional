package io.amichne.konditional.core.dsl.rules

import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.KonditionalDsl
import io.amichne.konditional.core.features.Feature

@KonditionalDsl
class RuleSet<RC : Context, T : Any, C, M : Namespace> @PublishedApi internal constructor(
    val feature: Feature<T, C, M>,
    internal val rules: List<RuleSpec<T, RC>>,
) where C : RC {
    operator fun plus(other: RuleSet<RC, T, C, M>): RuleSet<RC, T, C, M> =
        RuleSet(feature, rules + other.rules)

    companion object {
        fun <RC : Context, T : Any, C, M : Namespace> empty(
            feature: Feature<T, C, M>,
        ): RuleSet<RC, T, C, M> where C : RC =
            RuleSet(feature, emptyList())
    }
}

///**
// * Builds a rule set scoped to this feature using the feature's declared context type.
// *
// * This overload keeps call sites minimal when you do not need a contravariant
// * context type. The compiler infers all types from the feature receiver.
// */
//@JvmName("ruleSetDefault")
//fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.ruleSet(
//    build: RuleSetBuilder<T, C>.() -> Unit,
//): RuleSet<C, T, C, M> =
//    RuleSet(feature = this, rules = RuleSetBuilder<T, C>().apply(build).build())
//
///**
// * Builds a rule set using an explicit supertype context without reified generics.
// *
// * Use this when you want contravariant rule sets but prefer value-based
// * type selection at the call site:
// * ```kotlin
// * val global = feature.ruleSet(Context::class) { rule(value) { ios() } }
// * ```
// */
//@JvmName("ruleSetWithContextType")
//fun <T : Any, C, M : Namespace, RC : Context> Feature<T, C, M>.ruleSet(
//    @Suppress("UNUSED_PARAMETER") contextType: KClass<RC>,
//    build: RuleSetBuilder<T, RC>.() -> Unit,
//): RuleSet<RC, T, C, M> where C : RC =
//    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())
//
///**
// * Builds a rule set using a reified supertype context.
// *
// * Prefer this when you want contravariant context composition and a terse
// * call site:
// * ```kotlin
// * val global = feature.ruleSet<Context> { rule(value) { ios() } }
// * ```
// */
//inline fun <reified RC : Context, T : Any, C, M : Namespace> Feature<T, C, M>.ruleSet(
//    build: RuleSetBuilder<T, RC>.() -> Unit,
//): RuleSet<RC, T, C, M> where C : RC =
//    RuleSet(feature = this, rules = RuleSetBuilder<T, RC>().apply(build).build())
